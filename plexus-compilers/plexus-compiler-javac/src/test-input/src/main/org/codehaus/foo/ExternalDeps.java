package org.codehaus.foo;

import org.apache.commons.lang3.StringUtils;

public class ExternalDeps
{
	public void hello( String str )
	{
        System.out.println( StringUtils.upperCase( str)  );
	}
}
