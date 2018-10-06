/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                    List<String> pathValues = usedBy.stream()
                            .filter(operation -> operation.getTags() != null && !operation.getTags().isEmpty())
                            .map(operation -> context.getMarkupDocBuilder()
                                    .copy(false)
                                    .crossReference("paths", operation.getTags().get(0) +
                                                    "_resource",
                                            operation.getSummary())
                                    .toString())
                            .collect(toList());
                    if(pathValues.size() > 0){
                        context.getMarkupDocBuilder()
                                .unorderedList(pathValues);
                    }

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
