package com.eden.orchid.api.compilers;

import com.eden.orchid.api.registration.Prioritized;

/**
 * A generic compiler which can be used by a Theme to transform content. When a Theme is requested to compile a file of
 * a given type, it searches the list of registered Compilers and picks the one with the highest priority that is able
 * to compile the given file type.
 */
public abstract class OrchidCompiler extends Prioritized {

    /**
     * Compile content with a particular file extension using the optional provided data.
     *
     * @param extension the file extension that represents the type of data to compile
     * @param input     the content to be compiled
     * @param data      optional data to be passed to the compiler
     * @return the compiled content
     */
    public abstract String compile(String extension, String input, Object... data);

    /**
     * Gets the file extension representing the type of the output content.
     *
     * @return the output file extension
     */
    public abstract String getOutputExtension();

    /**
     * Get the list of file extensions this OrchidCompiler is able to process.
     *
     * @return the file extensions this OrchidCompiler can process
     */
    public abstract String[] getSourceExtensions();

    /**
     *
     * Get a list of patterns used to ignore files. Typically, this is to denote certain files as 'include-only', and
     * thus will help keep those files out of batch-compilations. Note that this matches the filename and extension
     * only, ignoring all path information.
     *
     * @return a list of regexes that match file names to ignore, or null if no files should be ignored
     */
    public String[] getIgnoredPatterns() {
        return null;
    }
}
