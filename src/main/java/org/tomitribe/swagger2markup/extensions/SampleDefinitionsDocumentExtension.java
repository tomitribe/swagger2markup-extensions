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
package org.tomitribe.swagger2markup.extensions;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.swagger2markup.internal.resolver.DefinitionDocumentResolverFromDefinition;
import io.github.swagger2markup.model.PathOperation;
import io.github.swagger2markup.spi.DefinitionsDocumentExtension;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static io.github.swagger2markup.internal.utils.ExamplesUtil.generateRequestExampleMap;
import static io.github.swagger2markup.internal.utils.ExamplesUtil.generateResponseExampleMap;
import static io.github.swagger2markup.internal.utils.RefUtils.computeSimpleRef;

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
        final Paths paths = this.globalContext.getSwagger().getPaths();

        final Optional<RequestBody> requestSample =
                paths.values()
                     .stream()
                     .map(PathItem::readOperations)
                     .flatMap(Collection::stream)
                     .map(Operation::getRequestBody)
                     .filter(Objects::nonNull)
                     .filter(bodyParameter -> bodyParameter.get$ref() != null)
                     .filter(bodyParameter -> computeSimpleRef(bodyParameter.get$ref()).equals(entity))
                     .findFirst();

        return requestSample
                .map(bodyParameter -> new PathOperation(null, null, new Operation().requestBody(bodyParameter)))
                .map(operation -> generateRequestExampleMap(true, operation,
                                                            globalContext.getSwagger().getComponents().getSchemas(),
                                                            new DefinitionDocumentResolverFromDefinition(globalContext),
                                                            context.getMarkupDocBuilder()))
                .flatMap(samples -> samples.values().stream().filter(Objects::nonNull).findFirst())
                .map(this::parseExample);
    }

    private Optional<String> findSampleInResponses(final Context context, final String entity) {
        final Paths paths = this.globalContext.getSwagger().getPaths();

        final Optional<ApiResponse> responseSample =
                paths.values()
                     .stream()
                     .map(PathItem::readOperations)
                     .flatMap(Collection::stream)
                     .map(Operation::getResponses)
                     .flatMap(apiResponses -> apiResponses.values().stream())
                     .filter(apiResponse -> Optional.ofNullable(apiResponse.getContent())
                                                    .map(Content::values)
                                                    .map(Collection::stream)
                                                    .flatMap(Stream::findFirst)
                                                    .filter(mediaType -> mediaType.getSchema()  != null && mediaType.getSchema().get$ref() != null)
                                                    .filter(mediaType -> computeSimpleRef(
                                                            mediaType.getSchema().get$ref()).equals(entity))
                                                    .isPresent())
                     .findFirst();

        return responseSample
                .map(response -> new PathOperation(null, null,
                                                   new Operation().responses(new ApiResponses()._default(response))))
                .map(operation -> generateResponseExampleMap(true, operation,
                                                             globalContext.getSwagger().getComponents().getSchemas(),
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
