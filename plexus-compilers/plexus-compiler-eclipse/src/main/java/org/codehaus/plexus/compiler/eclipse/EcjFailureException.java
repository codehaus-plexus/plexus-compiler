package org.codehaus.plexus.compiler.eclipse;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 22-4-18.
 */
public class EcjFailureException extends RuntimeException {
    private final String ecjOutput;

    public EcjFailureException(String ecjOutput) {
        super("Failed to run the ecj compiler: " + ecjOutput);
        this.ecjOutput = ecjOutput;
    }

    public String getEcjOutput()
    {
        return ecjOutput;
    }
}
