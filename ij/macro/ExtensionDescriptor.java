package ij.macro;

import ij.IJ;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class ExtensionDescriptor {
  public String name;
  public int[] argTypes;
  public MacroExtension handler;
  
  public ExtensionDescriptor(String theName, int[] theArgTypes, MacroExtension theHandler) {
    this.name = theName;
    this.argTypes = theArgTypes;
    this.handler = theHandler;
  }
  
  public static ExtensionDescriptor newDescriptor(String theName, MacroExtension theHandler, int[] types) {
    int[] argTypes = new int[types.length];
    for (int i=0; i < types.length; ++i) {
      argTypes[i] = types[i];
    }
    
    return new ExtensionDescriptor(theName, argTypes, theHandler);
  }
  
  public static ExtensionDescriptor newDescriptor(String theName, MacroExtension theHandler) {
    return newDescriptor(theName, theHandler, new int[0]);
  }
  
  public static ExtensionDescriptor newDescriptor(String theName, MacroExtension theHandler, int type) {
    return newDescriptor(theName, theHandler, new int[] {type});
  }
  
  public static ExtensionDescriptor newDescriptor(String theName, MacroExtension theHandler, int t1, int t2) {
    return newDescriptor(theName, theHandler, new int[] {t1, t2});
  }
  
  public static ExtensionDescriptor newDescriptor(String theName, MacroExtension theHandler, int t1, int t2, int t3) {
    return newDescriptor(theName, theHandler, new int[] {t1, t2, t3});
  }
  
  public static ExtensionDescriptor newDescriptor(String theName, MacroExtension theHandler, int t1, int t2, int t3, int t4) {
    return newDescriptor(theName, theHandler, new int[] {t1, t2, t3, t4});
  }
  
  private static class ReflectedHandler implements MacroExtension {
    String extName;
    Object instance;
    Method method;
    
    public ReflectedHandler(String theExtName, Object theInstance, Method theMethod) {
      this.extName = theExtName;
      this.instance = theInstance;
      this.method = theMethod;
    }
    
    /** This method intentionally returns nothing.  This object only handles calls.
     * 
     */  
    public ExtensionDescriptor[] getExtensionFunctions() {
      return new ExtensionDescriptor[0];
    }
    
    public String handleExtension(String name, Object[] args) {
      if (!name.equals(extName)) 
        throw new RuntimeException("Invalid handler for extension function "+name+" (only handles "+extName+")");
      
      try {
        Object result = method.invoke(instance, args);
        if (result==null) return null;
        return result.toString();
      } catch (Exception e) {
        e.printStackTrace();
        IJ.error("Error invoking extension function "+extName+": "+e.getMessage());
        return null;
      } 
    }
    
    
  }
  
  
  // look for methods starting with 'extfunc_' that have the correct arguments
  public static ExtensionDescriptor[] reflectFunctions(String theName, Object instance) {
    Method[] methods = instance.getClass().getMethods();
    
    ArrayList descriptors = new ArrayList();

    for (int i=0; i < methods.length; ++i) {
      String methodName = methods[i].getName();
      if (methodName.startsWith("extfunc_")) {
        String extName = methodName.substring(8);
        IJ.log("extension function "+extName);
        
        Class[] parameters = methods[i].getParameterTypes();
        
        int[] argTypes = new int[parameters.length];
        
        for (int j=0; j < parameters.length; ++j) {
          if (parameters[j] == Double.class || parameters[j] == Double.TYPE) {
            argTypes[j] = MacroExtension.ARG_NUMBER;
          } else if (parameters[j] == Double[].class || parameters[j] == double[].class) {
            argTypes[j] = MacroExtension.ARG_NUMBER+MacroExtension.ARG_OUTPUT;
          } else if (parameters[j] == String.class) {
            argTypes[j] = MacroExtension.ARG_STRING;
          } else if (parameters[j] == String[].class) {
            argTypes[j] = MacroExtension.ARG_STRING+MacroExtension.ARG_OUTPUT;
          } else if (parameters[j] == Object[].class) {
            argTypes[j] = MacroExtension.ARG_ARRAY;
          } else {
            throw new RuntimeException("Unsupported parameter type in extension function: "+parameters[j]);
          }
        }
        
        ReflectedHandler handler = new ReflectedHandler(extName, instance, methods[i]);
        ExtensionDescriptor desc = new ExtensionDescriptor(extName, argTypes, handler);
        descriptors.add(desc);
      }
    }
    
    return (ExtensionDescriptor[]) descriptors.toArray(new ExtensionDescriptor[0]);
  }
  
  public static ExtensionDescriptor newDescriptor(String theName, MacroExtension theHandler, Integer[] types) {
    int[] argTypes = new int[types.length];
    for (int i=0; i < types.length; ++i) {
      argTypes[i] = types[i].intValue();
    }
    
    return new ExtensionDescriptor(theName, argTypes, theHandler);
  }
  
  public static boolean isOptionalArg(int argType) {
    return (argType & MacroExtension.ARG_OPTIONAL) == MacroExtension.ARG_OPTIONAL;
  }
  
  public static boolean isOutputArg(int argType) {
    return (argType & MacroExtension.ARG_OUTPUT) == MacroExtension.ARG_OUTPUT;
  }
  
  public static int getRawType(int argType) {
    return argType & ~(MacroExtension.ARG_OUTPUT|MacroExtension.ARG_OPTIONAL);
  }
  
  public boolean checkArguments(Object[] args) {
    for (int i=0; i < argTypes.length; ++i) {
      boolean optional = isOptionalArg(argTypes[i]);
      boolean output   = isOutputArg(argTypes[i]);
      
      int rawType = getRawType(argTypes[i]);
      
      if (args.length < i)
        return optional ? true : false;
      
      switch(rawType) {
      case MacroExtension.ARG_STRING:
        if (output) {
          if (! (args[i] instanceof String[])) return false;
        } else {
          if (! (args[i] instanceof String)) return false;
        }
      case MacroExtension.ARG_NUMBER:
        if (output) {
          if (! (args[i] instanceof Double[])) return false;
        } else {
          if (!(args[i] instanceof Double)) return false;
        }
      case MacroExtension.ARG_ARRAY:
        if (!(args[i] instanceof Object[])) return false;
      }
    }
    
    return true;
  }
  
  public static String getTypeName(int argType) {
    switch(getRawType(argType)) {
    case MacroExtension.ARG_STRING:
      return "string";
    case MacroExtension.ARG_NUMBER:
      return "number";
    case MacroExtension.ARG_ARRAY:
      return "array";
    default:
      return "unknown";
    }
  }
  
  private static String getVariableTypename(int type) {
    switch (type) {
    case Variable.STRING:
      return "string";
    case Variable.NUMBER:
      return "number";
    case Variable.ARRAY:
      return "array";
    default:
      return "unknown";
    }
  }
  
  // TODO: this doesn't account for "loops" in the arrays, which will result in an infinite loop
  private static Object[] convertArray(Variable[] array) {
    Object[] oArray = new Object[ array.length ];
    for(int i=0; i < array.length; ++i) {
      switch(array[i].getType()) {
      case Variable.STRING:
        oArray[i] = array[i].getString();
        break;
      case Variable.VALUE:
        oArray[i] = new Double( array[i].getValue() );
        break;
      case Variable.ARRAY:
        oArray[i] = convertArray(array[i].getArray());
        break;
      default:
        oArray[i] = null;
      }
    }
    
    return oArray;
  }
  
  Variable[] parseArgumentList(Functions func) {
    Interpreter interp = func.interp;

    Variable[] vArray = new Variable[argTypes.length];
    int i=0;
    do {
      if (i >= argTypes.length) {
        interp.error("too many arguments (expected "+argTypes.length+")");
        return null;
      }
      boolean output   = isOutputArg(argTypes[i]);

      Variable v;
      if (output) {
        v = func.getVariable();
      } else {
        v = new Variable();
        
        switch (getRawType(argTypes[i])) {
        case MacroExtension.ARG_STRING:
          
          v.setString(func.getString());
          break;
        case MacroExtension.ARG_NUMBER:
          v.setValue(interp.getExpression());
          break;
        case MacroExtension.ARG_ARRAY:
          v.setArray(func.getArray());
          break;
        }
      }
      vArray[i] = v;
      ++i;
      interp.getToken();
    } while (interp.token == ',');
    
    if (interp.token!=')')
      interp.error("')' expected");

    if (i < argTypes.length && !isOptionalArg(argTypes[i])) {
      interp.error("too few arguments, expected "+argTypes.length+" but found "+i);
    }
    
    return vArray;
  }
 
  
  public static Object convertVariable(Interpreter interp, int rawType, Variable var) {
    int type = getRawType(rawType);
    boolean output = isOutputArg(rawType);

    if (var == null)
      return null;

    switch (type) {
    case MacroExtension.ARG_STRING:
      if (var.getType() != Variable.STRING) {
        interp.error("expected string, but variable type is "+getVariableTypename(var.getType()));
        return null;
      }
      if (output) {
        return new String[] { var.getString() };
      } else {
        return var.getString();
      }
    case MacroExtension.ARG_NUMBER:
      if (var.getType() != Variable.VALUE) {
        interp.error("expected number, but variable type is "+getVariableTypename(var.getType()));
        return null;
      }
      if (output) {
        return new Double[] { new Double(var.getValue()) };
      } else {
        return new Double( var.getValue() );
      }
    case MacroExtension.ARG_ARRAY:
      if (var.getType() != Variable.ARRAY) {
        interp.error("expected array, but variable type is "+getVariableTypename(var.getType()));
        return null;
      }
      return convertArray(var.getArray());
    default:
      interp.error("Unknown descriptor type "+type+" ("+rawType+")");
      return null;
    }
  }

  public static void convertOutputType(Variable variable, Object object) {
    if (object instanceof String[]) {
      String[] str = (String[]) object;
      variable.setString(str[0]);
    } else if (object instanceof Double[]) {
      Double[] dbl = (Double[]) object;
      variable.setValue(dbl[0].doubleValue());
    } else if (object instanceof Object[]) {
      Object[] arr = (Object[]) object;
      Variable[] vArr = new Variable[arr.length];
      for (int i=0; i < arr.length; ++i) {
        vArr[i] = new Variable();
        convertOutputType(vArr[i], arr[i]);
      }
      variable.setArray(vArr);
    }
  }
  
  public String dispatch(Functions func) {
    Interpreter interp = func.interp;

    if (argTypes.length==0) {
      interp.getParens();
      return handler.handleExtension(name, null);
    }
    interp.getLeftParen();
    
    Variable[] vArgs = null;
    int next = interp.nextNonEolToken();
    if (next != ')') {
      vArgs = parseArgumentList(func);
    }
    
    for (int i=0; i < vArgs.length; ++i) {
      Variable v = vArgs[i];
      System.err.println("variable is "+(v!= null?v.toString():"(null)"));
    }
    
    Object[] args = new Object[ argTypes.length ];
    // check variable types...
    for (int i=0; i < argTypes.length; ++i) {
      if (i >= vArgs.length) {
        if (!ExtensionDescriptor.isOptionalArg(argTypes[i])) {
          interp.error("expected argument "+(i+1)+" of type "+ExtensionDescriptor.getTypeName(argTypes[i]));
          return null;
        } else {
          break;
        }
      }
      
      args[i] = ExtensionDescriptor.convertVariable(interp, argTypes[i], vArgs[i]);
    }
    
    String retVal = handler.handleExtension(name, args);
    for (int i=0; i < argTypes.length; ++i) {
      if (i >= vArgs.length) break;
      if (ExtensionDescriptor.isOutputArg(argTypes[i])) {
        ExtensionDescriptor.convertOutputType(vArgs[i], args[i]);
      }
    }
    
    return retVal;
  }
}
