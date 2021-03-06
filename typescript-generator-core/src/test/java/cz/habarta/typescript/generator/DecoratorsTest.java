
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsDecorator;
import cz.habarta.typescript.generator.emitter.TsIdentifierReference;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.emitter.TsStringLiteral;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;


public class DecoratorsTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.mapClasses = ClassMapping.asClasses;
        settings.importDeclarations.add("import { JsonObject, JsonProperty } from \"json2typescript\"");
        settings.extensions.add(new ClassNameDecoratorExtension());
        settings.optionalProperties = OptionalProperties.useLibraryDefinition;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(City.class));
        Assert.assertTrue(output.contains("@JsonObject(\"City\")"));
        Assert.assertTrue(output.contains("@JsonProperty(\"name\", String)"));
    }

    private static class ClassNameDecoratorExtension extends Extension {

        @Override
        public EmitterExtensionFeatures getFeatures() {
            final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
            features.generatesRuntimeCode = true;
            return features;
        }

        @Override
        public List<TransformerDefinition> getTransformers() {
            return Arrays.asList(
                    new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, new ModelTransformer() {
                        @Override
                        public TsModel transformModel(SymbolTable symbolTable, TsModel model) {
                            return model.withBeans(model.getBeans().stream()
                                    .map(ClassNameDecoratorExtension.this::decorateClass)
                                    .collect(Collectors.toList())
                            );
                        }
                    })
            );
        }

        private TsBeanModel decorateClass(TsBeanModel bean) {
            if (!bean.isClass()) {
                return bean;
            }
            return bean
                    .withDecorators(Arrays.asList(new TsDecorator(
                            new TsIdentifierReference("JsonObject"),
                            Arrays.asList(new TsStringLiteral(bean.getOrigin().getSimpleName()))
                    )))
                    .withProperties(bean.getProperties().stream()
                            .map(ClassNameDecoratorExtension.this::decorateProperty)
                            .collect(Collectors.toList())
                    );
        }

        private TsPropertyModel decorateProperty(TsPropertyModel property) {
            return property
                    .withDecorators(Arrays.asList(new TsDecorator(
                            new TsIdentifierReference("JsonProperty"),
                            Arrays.asList(
                                    new TsStringLiteral(property.getName()),
                                    new TsIdentifierReference("String")
                            )
                    )));
        }

    }

    public static class City {
        public String name;
    }

}
