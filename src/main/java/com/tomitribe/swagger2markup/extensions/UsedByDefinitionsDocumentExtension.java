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

import io.github.swagger2markup.internal.utils.RefUtils;
import io.github.swagger2markup.spi.DefinitionsDocumentExtension;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class UsedByDefinitionsDocumentExtension extends DefinitionsDocumentExtension {
    @Override
    public void apply(final Context context) {
        if (Position.DEFINITION_AFTER.equals(context.getPosition())) {
            if (context.getDefinitionName().isPresent()) {
                final String entity = context.getDefinitionName().get();
                final Paths paths = this.globalContext.getSwagger().getPaths();

                final List<Operation> usedBy =
                        paths.values()
                             .stream()
                             .map(PathItem::readOperations)
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
        return Optional.ofNullable(operation.getRequestBody())
                       .map(RequestBody::getContent)
                       .map(content -> content.values().stream())
                       .flatMap(Stream::findFirst)
                       .map(MediaType::getSchema)
                       .filter(schema -> schema.get$ref() != null)
                       .filter(schema -> RefUtils.computeSimpleRef(schema.get$ref()).equals(entity))
                       .isPresent();
    }

    private static boolean hasEntityInResponse(final Operation operation, final String entity) {
        return operation.getResponses().values().stream()
                        .map(ApiResponse::getContent)
                        .filter(Objects::nonNull)
                        .flatMap(content -> content.values().stream())
                        .findFirst()
                        .map(MediaType::getSchema)
                        .filter(schema -> schema.get$ref() != null)
                        .filter(schema -> RefUtils.computeSimpleRef(schema.get$ref()).equals(entity))
                        .isPresent();
    }
}
