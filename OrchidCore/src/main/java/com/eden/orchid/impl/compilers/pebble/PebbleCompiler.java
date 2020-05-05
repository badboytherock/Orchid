package com.eden.orchid.impl.compilers.pebble;

import com.caseyjbrooks.clog.Clog;
import com.eden.orchid.Orchid;
import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.compilers.OrchidCompiler;
import com.eden.orchid.api.events.On;
import com.eden.orchid.api.events.OrchidEventListener;
import com.eden.orchid.api.resources.resource.OrchidResource;
import com.eden.orchid.utilities.OrchidExtensionsKt;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.extension.NodeVisitorFactory;
import com.mitchellbosecke.pebble.lexer.LexerImpl;
import com.mitchellbosecke.pebble.lexer.TokenStream;
import com.mitchellbosecke.pebble.node.RootNode;
import com.mitchellbosecke.pebble.parser.Parser;
import com.mitchellbosecke.pebble.parser.ParserImpl;
import com.mitchellbosecke.pebble.parser.ParserOptions;
import com.mitchellbosecke.pebble.template.PebbleTemplateImpl;
import kotlin.NotImplementedError;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public final class PebbleCompiler extends OrchidCompiler implements OrchidEventListener {

    private ExecutorService executor;
    private PebbleEngine engine;
    private Provider<OrchidContext> contextProvider;

    @Inject
    public PebbleCompiler(PebbleTemplateLoader loader, Set<Extension> extensions, Provider<OrchidContext> contextProvider) {
        super(10000);

        Extension[] extensionArray = new Extension[extensions.size()];
        extensions.toArray(extensionArray);

        this.executor = Executors.newFixedThreadPool(10);
        this.engine = new PebbleEngine.Builder()
                .loader(loader)
                .executorService(executor)
                .extension(extensionArray)
                .allowUnsafeMethods(true)
                .newLineTrimming(false)
                .build();

        this.engine.getExtensionRegistry().getAttributeResolver().add(new GetMethodAttributeResolver());
        this.contextProvider = contextProvider;
    }

    @Override
    public void compile(OutputStream os, @Nullable OrchidResource resource, String extension, String input, Map<String, Object> data) {
        try {
            LexerImpl lexer = new LexerImpl(
                    engine.getSyntax(),
                    engine.getExtensionRegistry().getUnaryOperators().values(),
                    engine.getExtensionRegistry().getBinaryOperators().values());
            TokenStream tokenStream = lexer.tokenize(new StringReader(input), "");

            Parser parser = new ParserImpl(
                    engine.getExtensionRegistry().getUnaryOperators(),
                    engine.getExtensionRegistry().getBinaryOperators(),
                    engine.getExtensionRegistry().getTokenParsers(),
                    new ParserOptions()
            );
            RootNode root = parser.parse(tokenStream);

            PebbleTemplateImpl compiledTemplate = new PebbleTemplateImpl(engine, root, "");

            for (NodeVisitorFactory visitorFactory : engine.getExtensionRegistry().getNodeVisitors()) {
                visitorFactory.createVisitor(compiledTemplate).visit(root);
            }

            ByteArrayOutputStream os1 = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(os1, StandardCharsets.UTF_8);
            compiledTemplate.evaluate(writer, data);
            writer.close();
            os.write(os1.toByteArray());

//            StringWriter writer = new StringWriter();
//            compiledTemplate.evaluate(writer, data);
//            writer.close();
//            os.write(writer.toString().getBytes());
        } catch (PebbleException e) {
            OrchidExtensionsKt.logSyntaxError(input, extension, e.getLineNumber(), 0, e.getMessage());
        } catch (Exception e) {
            Clog.e("Error rendering Pebble template (see template source below)", e);
            Clog.e(input);
            contextProvider.get().diagnosisMessage(() -> ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public String getOutputExtension() {
        return "html";
    }

    @Override
    public String[] getSourceExtensions() {
        return new String[]{"peb", "pebble"};
    }

// Clean up executor on shutdown
//----------------------------------------------------------------------------------------------------------------------

    @On(Orchid.Lifecycle.Shutdown.class)
    public void onEndSession(Orchid.Lifecycle.Shutdown event) {
        executor.shutdown();
    }

    @On(Orchid.Lifecycle.ClearCache.class)
    public void onClearCache(Orchid.Lifecycle.ClearCache event) {
        engine.getTagCache().invalidateAll();
        engine.getTemplateCache().invalidateAll();
    }

}
