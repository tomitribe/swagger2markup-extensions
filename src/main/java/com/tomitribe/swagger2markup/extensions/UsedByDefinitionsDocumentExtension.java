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

import io.github.swagger2markup.spi.DefinitionsDocumentExtension;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.properties.RefProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class UsedByDefinitionsDocumentExtension extends DefinitionsDocumentExtension {
    @Override
    public void apply(final Context context) {
        if (Position.DEFINITION_AFTER.equals(context.getPosition())) {
            if (context.getDefinitionName().isPresent()) {
                final String entity = context.getDefinitionName().get();
                final Map<String, Path> paths = this.globalContext.getSwagger().getPaths();

                final List<Operation> usedBy =
                        paths.values()
                             .stream()
                             .map(Path::getOperations)
                             .flatMap(Collection::stream)
                             .filter(operation -> hasEntity(operation, entity))
                             .collect(toList());

                if (!usedBy.isEmpty()) {
                    context.getMarkupDocBuilder().sectionTitleLevel3("Used By");
                    context.getMarkupDocBuilder()
                           .unorderedList(usedBy.stream()
                                                .filter(operation -> !operation.getTags().isEmpty())
                                                .map(operation -> context.getMarkupDocBuilder()
                                                                         .copy(false)
                                                                         .crossReference("paths", operation.getTags().get(0) +
                                                                                         "_resource",
                                                                                         operation.getSummary())
                                                                         .toString())
                                                .collect(toList()));
                }
            }
        }
    }

    private static boolean hasEntity(final Operation operation, final String entity) {
        return hasEntityInRequest(operation, entity) || hasEntityInResponse(operation, entity);
    }

    private static boolean hasEntityInRequest(final Operation operation, final String entity) {
        return operation.getParameters()
                        .stream()
                        .filter(parameter -> parameter instanceof BodyParameter)
                        .map(parameter -> (BodyParameter) parameter)
                        .filter(bodyParameter -> bodyParameter.getSchema() != null)
                        .filter(bodyParameter -> bodyParameter.getSchema() instanceof RefModel)
                        .anyMatch(
                                bodyParameter -> ((RefModel) bodyParameter.getSchema()).getSimpleRef().equals(entity));

    }

    private static boolean hasEntityInResponse(final Operation operation, final String entity) {
        return operation.getResponses().values().stream()
                        .filter(response -> response.getSchema() != null)
                        .filter(response -> response.getSchema().getType().equals("ref"))
                        .anyMatch(response -> ((RefProperty) response.getSchema()).getSimpleRef().equals(entity));
    }
}
