package com.marmot.remote.demo.test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import com.marmot.remote.demo.AbstractParamterController;
import com.marmot.remote.demo.InterfaceParameterController;


public class AppTest {
	  public static void main(String[] args) {
	        ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
	        Method[] methods = InterfaceParameterController.class.getMethods();

	        for (int i = 0; i < methods.length; i++) {
	            String[] paramterNames = parameterNameDiscoverer.getParameterNames(methods[i]);
	            
	            System.out.println(methods[i].getName());
	            
	            //Parameter[] parameters = methods[i].getParameters();
	            if(null!=paramterNames){
	            	for(int j=0;j<paramterNames.length;j++){
	            		System.out.println(paramterNames[j]);
	            	}
	            }
	            
	            System.out.println("********************************");
	        }
	    }
}
