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

import io.github.swagger2markup.spi.DefinitionsDocumentExtension;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class SeeAlsoDefinitionsDocumentExtension extends DefinitionsDocumentExtension {
    private static final List<String> KEYWORDS = Stream.of("Abstract", "Create", "Update").collect(toList());

    @Override
    public void apply(final Context context) {
        if (Position.DEFINITION_AFTER.equals(context.getPosition())) {
            final Map<String, Schema> definitions = globalContext.getSwagger().getComponents().getSchemas();

            if (context.getDefinitionName().isPresent()) {
                final String entity = parseActualEntity(context.getDefinitionName().get());

                final List<String> relatedEntities =
                        KEYWORDS.stream()
                                .map(keyword -> StringUtils.appendIfMissing(keyword, entity))
                                .collect(toList());

                final List<String> seeAlso =
                        definitions.keySet()
                                   .stream()
                                   .filter(relatedEntities::contains)
                                   .collect(toList());

                if (!context.getDefinitionName().get().equals(entity)) {
                    seeAlso.add(entity);
                }
                Optional.ofNullable(definitions.get(entity + "s")).ifPresent(model -> seeAlso.add(entity + "s"));

                if (!seeAlso.isEmpty()) {
                    context.getMarkupDocBuilder().sectionTitleLevel3("See Also");
                    context.getMarkupDocBuilder()
                           .unorderedList(seeAlso.stream()
                                                 .map(reference -> context.getMarkupDocBuilder()
                                                                          .copy(false)
                                                                          .crossReference(reference, reference)
                                                                          .toString())
                                                 .collect(toList()));
                }
            }
        }
    }

    private String parseActualEntity(final String name) {
        return KEYWORDS.stream()
                       .filter(name::startsWith)
                       .findFirst()
                       .map(keyword -> StringUtils.removeStart(name, keyword))
                       .orElse(name);
    }
}
