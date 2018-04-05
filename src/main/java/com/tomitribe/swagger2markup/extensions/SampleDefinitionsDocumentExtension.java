/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.tomitribe.swagger2markup.extensions;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.swagger2markup.internal.resolver.DefinitionDocumentResolverFromDefinition;
import io.github.swagger2markup.model.PathOperation;
import io.github.swagger2markup.spi.DefinitionsDocumentExtension;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.swagger2markup.internal.utils.ExamplesUtil.generateRequestExampleMap;
import static io.github.swagger2markup.internal.utils.ExamplesUtil.generateResponseExampleMap;

public class SampleDefinitionsDocumentExtension extends DefinitionsDocumentExtension {
    @Override
    public void apply(final Context context) {
        if (Position.DEFINITION_AFTER.equals(context.getPosition())) {
            if (context.getDefinitionName().isPresent()) {
                final String entity = context.getDefinitionName().get();

                Stream.of(findSampleInRequests(context, entity), findSampleInResponses(context, entity))
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .findFirst()
                      .ifPresent(example -> {
                          context.getMarkupDocBuilder().sectionTitleLevel3("Sample");
                          context.getMarkupDocBuilder().listingBlock(example, "json");
                      });
            }
        }
    }

    private Optional<String> findSampleInRequests(final Context context, final String entity) {
        final Map<String, Path> paths = this.globalContext.getSwagger().getPaths();

        final Optional<BodyParameter> requestSample =
                paths.values()
                     .stream()
                     .map(Path::getOperations)
                     .flatMap(Collection::stream)
                     .map(Operation::getParameters)
                     .flatMap(Collection::stream)
                     .filter(parameter -> parameter instanceof BodyParameter)
                     .map(parameter -> (BodyParameter) parameter)
                     .filter(bodyParameter -> bodyParameter.getSchema() != null)
                     .filter(bodyParameter -> bodyParameter.getSchema() instanceof RefModel)
                     .filter(bodyParameter -> ((RefModel) bodyParameter.getSchema()).getSimpleRef().equals(entity))
                     .findFirst();

        return requestSample
                .map(bodyParameter -> new PathOperation(null, null, new Operation().parameter(bodyParameter)))
                .map(operation -> generateRequestExampleMap(true, operation,
                                                            globalContext.getSwagger().getDefinitions(),
                                                            new DefinitionDocumentResolverFromDefinition(globalContext),
                                                            context.getMarkupDocBuilder()))
                .flatMap(samples -> samples.values().stream().filter(Objects::nonNull).findFirst())
                .map(this::parseExample);
    }

    private Optional<String> findSampleInResponses(final Context context, final String entity) {
        final Map<String, Path> paths = this.globalContext.getSwagger().getPaths();

        final Optional<Response> responseSample =
                paths.values()
                     .stream()
                     .map(Path::getOperations)
                     .flatMap(Collection::stream)
                     .map(Operation::getResponses)
                     .map(Map::values)
                     .flatMap(Collection::stream)
                     .filter(response -> response.getSchema() != null)
                     .filter(response -> response.getSchema().getType().equals("ref"))
                     .filter(response -> ((RefProperty) response.getSchema()).getSimpleRef().equals(entity))
                     .findFirst();

        return responseSample
                .map(response -> new PathOperation(null, null, new Operation().response(0, response)))
                .map(operation -> generateResponseExampleMap(true, operation,
                                                             globalContext.getSwagger().getDefinitions(),
                                                             new DefinitionDocumentResolverFromDefinition(
                                                                     globalContext),
                                                             context.getMarkupDocBuilder()))
                .flatMap(samples -> samples.values().stream().filter(Objects::nonNull).findFirst())
                .map(this::parseExample);
    }

    private String parseExample(final Object raw) throws RuntimeException {
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            return Json.pretty(mapper.readTree(Json.pretty(raw)));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read example", ex);
        }
    }
}
