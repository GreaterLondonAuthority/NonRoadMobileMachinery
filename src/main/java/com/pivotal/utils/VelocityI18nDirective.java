/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.utils;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.directive.DirectiveConstants;
import org.apache.velocity.runtime.parser.node.ASTBlock;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a custom directive for Velocity to enable internationalisation
 * of strings in the templates
 * <p>
 * The directive takes the form of #I18N(message, [param1, paramX])<br>
 * where message is the name of the token to translate<br>
 * param1-X are any replacement parameter values required in the token content
 * </p>
 * The rules for the token selection are defined in I18n.translate()
 */
public class VelocityI18nDirective extends Directive {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VelocityI18nDirective.class);

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "I18N";
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return DirectiveConstants.LINE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

        // loop through all "params"

        String stringToTranslate = null;
        List<Object> parameters = new ArrayList<>();
        try {
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                if (node.jjtGetChild(i) != null) {
                    if (!(node.jjtGetChild(i) instanceof ASTBlock)) {

                        // Reading and casting inline parameters

                        if (i == 0) stringToTranslate = node.jjtGetChild(i).value(context) + "";
                        else parameters.add(node.jjtGetChild(i).value(context));
                    }
                }
                else {

                    // Reading block content and rendering it

                    StringWriter blockContent = new StringWriter();
                    node.jjtGetChild(i).render(context, blockContent);
                    break;
                }
            }

            // Translate the message using the parameters passed in the call

            writer.write(I18n.getString(I18n.getLocale(context), stringToTranslate, parameters.toArray()));
        }
        catch (Exception e) {
            writer.write(String.format("ERROR translating [%s] - %s", stringToTranslate, PivotalException.getErrorMessage(e)));
        }
        return true;
    }
}
