package io.canvasmc.canvas.config;

import io.canvasmc.canvas.config.annotation.AlwaysAtTop;
import io.canvasmc.canvas.config.yaml.org.yaml.snakeyaml.DumperOptions;
import io.canvasmc.canvas.config.yaml.org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unchecked", "rawtypes"})
public class AnnotationBasedYamlSerializer<T> implements ConfigSerializer<T> {
    private static final String NEW_LINE = "\n";
    private final Map<Class<? extends Annotation>, AnnotationContextProvider> annotationContextProviderRegistry = new LinkedHashMap<>();
    private final Map<Class<? extends Annotation>, AnnotationValidationProvider> annotationValidationProviderRegistry = new LinkedHashMap<>();
    private final List<Consumer<PostSerializeContext<T>>> postConsumerContexts = new LinkedList<>();
    private final Configuration definition;
    private final Class<T> configClass;
    private final Yaml yaml;
    private final List<Pair<Pattern, RuntimeModifier<?>>> runtimeModifiers = new LinkedList<>();
    private final Consumer<String> logger;
    // only available for builders
    private String[] header;

    public AnnotationBasedYamlSerializer(Configuration definition, @NotNull Class<T> configClass, Yaml yaml, Consumer<String> logger) {
        this.definition = definition;
        this.configClass = configClass;
        this.yaml = yaml;
        this.header = new String[0];
        this.logger = logger == null ? System.out::println : logger;
    }

    public AnnotationBasedYamlSerializer(Configuration definition, Class<T> configClass, Consumer<String> logger) {
        this(definition, configClass, new Yaml(), logger);
    }

    public AnnotationBasedYamlSerializer(SerializationBuilder.@NotNull Final builder, Consumer<String> logger) {
        this(builder.definition(), (Class<T>) builder.owningClass(), logger);
        this.annotationContextProviderRegistry.putAll(builder.wrappedContextMap());
        this.annotationValidationProviderRegistry.putAll(builder.wrappedValidationMap());
        this.postConsumerContexts.addAll(builder.postConsumers());
        this.header = builder.header();
        this.runtimeModifiers.addAll(builder.runtimeModifiers());
    }

    /**
     * Registers an annotation handler, which acts as a pre-processor before adding the key and its value to the yaml during serialization to disk
     *
     * @param annotation      the class of the annotation you are registering
     * @param contextProvider the context provider of the annotation you are registering
     */
    @Deprecated(forRemoval = true)
    public <A extends Annotation> void registerAnnotationHandler(@NotNull Class<A> annotation, AnnotationContextProvider<A> contextProvider) {
        if (!annotation.isAnnotation()) {
            throw new IllegalArgumentException("Class must be an annotation");
        }
        annotationContextProviderRegistry.put(annotation, contextProvider);
    }

    /**
     * Registers a validation handler, which acts as a post-processor after writing to disk to validate entries provided by the configuration
     *
     * @param annotation         the class of the annotation you are registering
     * @param validationProvider the validation provider of the annotation you are registering
     */
    @Deprecated(forRemoval = true)
    public <A extends Annotation> void registerAnnotationValidator(@NotNull Class<A> annotation, AnnotationValidationProvider<A> validationProvider) {
        if (!annotation.isAnnotation()) {
            throw new IllegalArgumentException("Class must be an annotation");
        }
        annotationValidationProviderRegistry.put(annotation, validationProvider);
    }

    /**
     * Post-serialize action. This triggers immediately after the config is written to disk, and acts as a post-processor for the config
     *
     * @param contextConsumer the consumer for the {@link PostSerializeContext}
     */
    @Deprecated(forRemoval = true)
    public void registerPostSerializeAction(Consumer<PostSerializeContext<T>> contextConsumer) {
        this.postConsumerContexts.add(contextConsumer);
    }

    private @NotNull Map<String, Object> sortByKeys(@NotNull Map<String, Field> keysOrder, Map<String, Object> data) {
        Map<String, Object> rebuiltData = new LinkedHashMap<>();

        for (final String fullKey : keysOrder.keySet()) {
            String[] keys = fullKey.split("\\.");
            Map<String, Object> currentMap = rebuiltData;
            Map<?, ?> latest = data;
            String alwaysAtTopKey = null;
            Object alwaysAtTopValue = null;

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                if (i == keys.length - 1) {
                    Object value = latest.get(key);
                    if (keysOrder.get(fullKey).isAnnotationPresent(AlwaysAtTop.class)) {
                        alwaysAtTopKey = key;
                        alwaysAtTopValue = value;
                    } else {
                        currentMap.put(key, value);
                    }
                    break;
                }

                Object next = latest.get(key);
                if (next instanceof Map<?, ?> nextMap) {
                    latest = nextMap;
                }

                currentMap = (Map<String, Object>) currentMap.computeIfAbsent(key, _ -> new LinkedHashMap<>());
            }

            if (alwaysAtTopKey != null) {
                Map<String, Object> parentMap = rebuiltData;
                for (int i = 0; i < keys.length - 1; i++) {
                    parentMap = (Map<String, Object>) parentMap.get(keys[i]);
                }
                Map<String, Object> reorderedMap = new LinkedHashMap<>();
                reorderedMap.put(alwaysAtTopKey, alwaysAtTopValue);
                reorderedMap.putAll(parentMap);
                parentMap.clear();
                parentMap.putAll(reorderedMap);
            }
        }

        return rebuiltData;
    }

    private void forEach(@NotNull Map<String, Object> data, Map<String, Field> fields, String parentKey, TriConsumer<String, Field, Object> consumer) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;

            switch (value) {
                case null -> {
                }
                case Map _ -> forEach((Map<String, Object>) value, fields, fullKey, consumer);
                case List<?> list -> {
                    if (!list.isEmpty()) {
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> itemMap) {
                                forEach((Map<String, Object>) itemMap, fields, fullKey, consumer);
                            } else {
                                consumer.accept(fullKey, fields.get(fullKey), item);
                            }
                        }
                    }
                }
                default -> consumer.accept(fullKey, fields.get(fullKey), value);
            }

        }
    }

    private void writeYaml(StringWriter writer, @NotNull Map<String, Object> data, Map<String, Field> fields, int indentLevel, String parentKey) {
        String indent = "   ".repeat(indentLevel);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;

            if (value == null) {
                continue;
            }

            this.annotationContextProviderRegistry.forEach((annotationClass, contextProvider) -> {
                Field keyField = fields.get(fullKey);
                if (keyField == null) {
                    return;
                }
                if (keyField.isAnnotationPresent(annotationClass)) {
                    contextProvider.apply(writer, indent, fullKey, keyField, keyField.getAnnotation(annotationClass));
                }
            });

            if (value instanceof Map) {
                writer.append(indent).append(key).append(":\n");
                writeYaml(writer, (Map<String, Object>) value, fields, indentLevel + 1, fullKey);
            } else if (value instanceof List<?> list) {
                writer.append(indent).append(key).append(":");
                if (!list.isEmpty()) {
                    writer.append(NEW_LINE);
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> itemMap) {
                            writer.append(indent).append("   -\n");
                            writeYaml(writer, (Map<String, Object>) itemMap, fields, indentLevel + 2, fullKey);
                        } else {
                            writer.append(indent).append("   - ").append(item instanceof String ? "\"" + item + "\"" : item.toString()).append(NEW_LINE);
                        }
                    }
                } else {
                    writer.append(" []\n");
                }
            } else {
                writer.append(indent).append(key).append(": ").append(value.toString()).append(NEW_LINE);
            }
        }
    }

    private <V> V applyRuntimeModifiers(String path, V value) {
        for (Pair<Pattern, RuntimeModifier<?>> pair : runtimeModifiers) {
            if (pair.a().matcher(path).matches()) {
                RuntimeModifier<V> modifier = (RuntimeModifier<V>) pair.b();
                Class<?> expected = wrapPrimitive(modifier.classType());
                Class<?> actual = wrapPrimitive(value.getClass());

                if (expected.isAssignableFrom(actual)) {
                    value = modifier.modifier().apply(value);
                }
            }
        }
        return value;
    }

    private static Class<?> wrapPrimitive(@NotNull Class<?> clazz) {
        if (!clazz.isPrimitive()) return clazz;
        return switch (clazz.getName()) {
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            case "boolean" -> Boolean.class;
            case "char" -> Character.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            default -> clazz;
        };
    }

    private void applyModifiersRecursive(String currentPath, Object node, Set<Object> visited) {
        if (node == null || visited.contains(node)) return;
        visited.add(node);

        Class<?> clazz = node.getClass();
        for (Field field : clazz.getFields()) {
            if (field.accessFlags().contains(AccessFlag.STATIC)) continue; // don't run on static fields
            String path = currentPath.isEmpty() ? field.getName() : currentPath + "." + field.getName();
            try {
                Object value = field.get(node);
                Object newValue = applyRuntimeModifiers(path, value);
                if (newValue != value) {
                    field.set(node, newValue);
                }
                if (newValue != null && !isPrimitiveOrWrapper(newValue.getClass()) && !(newValue instanceof String)) {
                    applyModifiersRecursive(path, newValue, visited);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private boolean isPrimitiveOrWrapper(@NotNull Class<?> type) {
        return type.isPrimitive() ||
            type == Boolean.class || type == Byte.class || type == Character.class ||
            type == Short.class || type == Integer.class || type == Long.class ||
            type == Float.class || type == Double.class;
    }

    private @NotNull Path getConfigPath() {
        return getConfigFolder().resolve(this.definition.value() + ".yml");
    }

    @Override
    public void serialize(T config) throws SerializationException {
        Path configPath = this.getConfigPath();

        try {
            Files.createDirectories(configPath.getParent());
            // Build yaml content
            Map<String, Field> keyToField = ConfigurationUtils.FIELD_MAP;
            Map<String, Object> data = buildYamlData(config);

            // Reorder data based on keyset
            Map<String, Object> reorderedData = sortByKeys(keyToField, data);

            StringWriter yamlWriter = new StringWriter();
            if (this.header.length > 0) {
                for (final String line : this.header) {
                    yamlWriter.append("## ").append(line).append(NEW_LINE);
                }
                yamlWriter.append(NEW_LINE);
            }
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            // Write annotation handlers to yaml
            writeYaml(yamlWriter, reorderedData, keyToField, 0, "");

            // Print diff
            // Get flattened key sets
            Map<String, Object> newData = buildYamlData(config); // This is what we're about to write
            Map<String, Object> oldData = new LinkedHashMap<>();
            if (Files.exists(configPath)) {
                String oldYaml = Files.readString(configPath);
                oldData = this.yaml.load(oldYaml);
                if (oldData == null) oldData = new LinkedHashMap<>(); // Fallback if file was empty
            }

            Set<String> newKeys = flattenKeys(newData, "");
            Set<String> oldKeys = flattenKeys(oldData, "");

            Set<String> addedKeys = new java.util.HashSet<>(newKeys);
            addedKeys.removeAll(oldKeys);

            Set<String> removedKeys = new java.util.HashSet<>(oldKeys);
            removedKeys.removeAll(newKeys);

            for (String key : addedKeys) {
                this.logger.accept("New configuration option added, \"" + key + "\" in config " + this.definition.value());
            }
            for (String key : removedKeys) {
                this.logger.accept("Configuration option, \"" + key + "\" was removed in config " + this.definition.value());
            }
            // Write to disk
            Files.writeString(configPath, yamlWriter.toString());
            T deserialized = deserialize();
            // Validate entries
            data = buildYamlData(deserialized);
            forEach(data, keyToField, "", (key, field, value) -> {
                if (field == null) {
                    throw new RuntimeException("Field for associated key '" + key + "' was null!");
                }
                this.annotationValidationProviderRegistry.forEach((annotation, validator) -> {
                    if (field.isAnnotationPresent(annotation)) {
                        try {
                            validator.validate(key, field, field.getAnnotation(annotation), value);
                        } catch (ValidationException exception) {
                            throw new RuntimeException("Field " + key + " did not pass validation of " + annotation.getSimpleName() + " for reason of '" + exception.getMessage() + "'");
                        }
                    }
                });
            });
            applyModifiersRecursive("", deserialized, Collections.newSetFromMap(new IdentityHashMap<>()));
            PostSerializeContext context = new PostSerializeContext<>(configPath, deserialized, createDefault(), yamlWriter.toString());
            this.postConsumerContexts.forEach(consumer -> consumer.accept(context));
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    private @NotNull Set<String> flattenKeys(@NotNull Map<String, Object> map, String prefix) {
        Set<String> keys = new java.util.HashSet<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(fullKey);
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> subMap) {
                keys.addAll(flattenKeys((Map<String, Object>) subMap, fullKey));
            }
        }
        return keys;
    }

    private Map<String, Object> buildYamlData(T config) {
        String yamlContent = this.yaml.dump(config);
        String[] lines = yamlContent.split(NEW_LINE, 2);
        String body = lines.length > 1 ? lines[1] : "";

        Yaml yaml = new Yaml();
        return yaml.load(new StringReader(body));
    }

    @Override
    public T deserialize() throws SerializationException {
        Path configPath = this.getConfigPath();
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                return this.yaml.load("!!" + getConfigClass().getName() + NEW_LINE + content);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        } else {
            return this.createDefault();
        }
    }

    @Override
    public T createDefault() {
        return constructUnsafely(this.configClass);
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public Yaml getYaml() {
        return yaml;
    }

    public record PostSerializeContext<A>(Path configPath, A configuration, A defaultConfiguration, String contents) {

    }
}
