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
