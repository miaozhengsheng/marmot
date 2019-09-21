package com.marmot.framework;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
    
    public static void main(String[] args) {
        ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
        Method[] methods = TestParamteraNameInterfaceImpl.class.getMethods();

        for (int i = 0; i < methods.length; i++) {
            String[] paramterNames = parameterNameDiscoverer.getParameterNames(methods[i]);
            
            System.out.println(methods[i].getName());
            
            Parameter[] parameters = methods[i].getParameters();
            if(null!=paramterNames){
            	for(int j=0;j<paramterNames.length;j++){
            		System.out.println(paramterNames[j]);
            	}
            }
            
            System.out.println("********************************");
        }
    }
}
