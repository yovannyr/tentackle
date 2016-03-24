/**
 * Tentackle - a framework for java desktop applications
 * Copyright (C) 2001-2008 Harald Krake, harald@krake.de, +49 7722 9508-0
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// $Id: AnalyzeInfo.java 410 2008-09-03 12:42:26Z harald $

package org.tentackle.annotations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Holds information gathered by the Analyze annotation.
 * <p>
 * The AnalyzeProcessor creates an info file in an apt run and
 * wurblets pick up this information to generate code.
 *
 * @author harald
 */
public class AnalyzeInfo {
  
  /**
   * Fileformat version
   */
  public static final String INFO_FILE_VERSION = "1.0";
  
  /**
   * all info files end with .info
   */
  public static final String INFO_FILE_EXTENSION = ".info";
  
  // currently AnalyzeInfo supports only the type METHOD, but other types will follow.
  
  /**
   * Information type for a method.
   */
  public static final String TYPE_METHOD = "method";
  
  /**
   * Information type for a class.
   */
  public static final String TYPE_CLASS = "class";    // not implemented yet!
  
  
  // some private class Strings (we don't use <class>.getName() because we want to keep dependencies small
  private static final String CLASSNAME_COLLECTION  = "java.util.Collection";
  
  private static final String CLASSNAME_DB          = "org.tentackle.db.Db";
  private static final String CLASSNAME_DBOBJECT    = "org.tentackle.db.DbObject";
  private static final String CLASSNAME_DBCURSOR    = "org.tentackle.db.DbCursor";
  
  private static final String CLASSNAME_CONTEXTDB   = "org.tentackle.appworx.ContextDb";
  private static final String CLASSNAME_APPDBOBJECT = "org.tentackle.appworx.AppDbObject";
  
  
  /**
   * Abstraction of a method parameter.
   */
  public static class Parameter {
    
    private String name;                    // the formal parameter name
    private String type;                    // the parameter's type
    private Modifier[] modifiers;           // modifiers like final, etc...
    private boolean instanceOfDb;           // true if paramater is an instance of a Db
    private boolean instanceOfContextDb;    // true if parameter is an instance of a ContextDb
    private boolean instanceOfDbObject;     // true if parameter is an instance of a DbObject
    private boolean instanceOfAppDbObject;  // true if parameter is an instance of a AppDbObject
    
    private boolean varArg;                 // true if this is a (last) vararg
    
    
    /**
     * Constructor used in apt run.
     * 
     * @param processingEnv the annotations processor's environment
     * @param var the variable element
     */
    public Parameter(ProcessingEnvironment processingEnv, VariableElement var) {
      
      TypeMirror tm = var.asType();
      
      name      = var.toString();
      type      = tm.toString();
      modifiers = var.getModifiers().toArray(new Modifier[0]);
      
      // determine special types
      instanceOfDb          = isTypeInstanceoOf(processingEnv, tm, CLASSNAME_DB);
      instanceOfContextDb   = isTypeInstanceoOf(processingEnv, tm, CLASSNAME_CONTEXTDB);
      instanceOfAppDbObject = isTypeInstanceoOf(processingEnv, tm, CLASSNAME_APPDBOBJECT);
      if (instanceOfAppDbObject) {
        instanceOfDbObject = true;
      }
      else  {
        instanceOfDbObject    = isTypeInstanceoOf(processingEnv, tm, CLASSNAME_DBOBJECT);
      }
    }
    
    
    /**
     * Constructor used when reading from infofile.
     * 
     * @param stok the string tokenizer (of the infofile)
     */
    public Parameter(StringTokenizer stok) {
      name                  = stok.nextToken();
      type                  = stok.nextToken();
      instanceOfDb          = Boolean.valueOf(stok.nextToken());
      instanceOfContextDb   = Boolean.valueOf(stok.nextToken());
      instanceOfDbObject    = Boolean.valueOf(stok.nextToken());
      instanceOfAppDbObject = Boolean.valueOf(stok.nextToken());
      modifiers             = readModifiers(stok);
    }
    
    /**
     * Constructor for wurblets adding paramaters like "this".
     * 
     * @param name the formal parameter name
     * @param type the parameter type
     */
    public Parameter(String name, String type) {
      this.name = name;
      this.type = type;
    }
    
    
    /**
     * Gets the formal parameter name
     * @return the parameter name
     */
    public String getName() {
      return name;
    }
    
    /**
     * Gets the parameter type
     * @return the parameter type
     */
    public String getType() {
      return varArg ? (type.substring(0, type.length() - 2) + "...") : type;
    }
    
    /**
     * Gets the modifiers
     * @return the array of modifiers
     */
    public Modifier[] getModifiers() {
      return modifiers;
    }
    
    /**
     * Returns whether the parameter is an instance of a {@link org.tentackle.db.Db}.
     * @return true if instanceof Db
     */
    public boolean isInstanceOfDb() {
      return instanceOfDb;
    }
    
    /**
     * Returns whether the parameter is an instance of a {@link org.tentackle.appworx.ContextDb}.
     * @return true if instanceof ContextDb
     */
    public boolean isInstanceOfContextDb() {
      return instanceOfContextDb;
    }
    
    /**
     * Returns whether the parameter is an instance of a {@link org.tentackle.db.DbObject}.
     * @return true if instanceof DbObject
     */
    public boolean isInstanceOfDbObject() {
      return instanceOfDbObject;
    }
    
    /**
     * Gets all modifiers as a single string.
     * 
     * @return all modifiers as string
     */
    public String getModifiersAsString() {
      return objectArrayToString(modifiers, " ");
    }

    
    /**
     * Checks if a given modifier is set.
     * 
     * @param modifier the modifier to test
     * @return true modifier set
     */
    public boolean isModifierSet(Modifier modifier) {
      return AnalyzeInfo.isModifierSet(modifier, modifiers);
    }
    
    /**
     * Writes this parameter to the infofile.
     * 
     * @param writer the writer bound to the infofile
     * @throws java.io.IOException
     */
    public void write(PrintWriter writer) throws IOException {
      StringBuilder buf = new StringBuilder();
      buf.append(name);
      buf.append(' ');
      buf.append(type);
      buf.append(' ');
      buf.append(Boolean.toString(instanceOfDb));
      buf.append(' ');
      buf.append(Boolean.toString(instanceOfContextDb));
      buf.append(' ');
      buf.append(Boolean.toString(instanceOfDbObject));
      buf.append(' ');
      buf.append(Boolean.toString(instanceOfAppDbObject));
      String modStr = getModifiersAsString();
      if (modStr.length() > 0) {
        buf.append(' ');
        buf.append(modStr);
      }
      writer.print(buf.toString());
    }
    
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append(getModifiersAsString());
      if (buf.length() > 0) {
        buf.append(' ');
      }
      buf.append(getType());
      buf.append(' ');
      buf.append(name);
      return buf.toString();
    }
  }
  
  
  
  
  
  
  // information gathered from apt run
  private String type;                            // one of TYPE_...
  private String className;                       // name of the class
  private String superClassName;                  // name of super class
  private String methodName;                      // name of the method
  private String returnType;                      // the methods return type
  private boolean returningDbObject;              // true if method returns a DbObject
  private boolean returningDbObjectCollection;    // true if method returns a collection of DbObjects
  private boolean returningDbCursor;              // true if method returns a DbCursor
  private boolean returningAppDbObject;           // true if method returns an AppDbObject
  private boolean returningAppDbObjectCollection; // true if method returns a collection of AppDbObjects
  private boolean returningAppDbCursor;           // true if method returns an AppDbCursor
  private List<Parameter> parameterList;          // formal parameters
  private boolean varArgsMethod;                  // true if varargs method
  private Modifier[] modifiers;                   // modifiers like static, public, etc...
  
  
  
  /**
   * Creates an AnalyzeInfo for a given type.
   * Used when reading from infofile.
   *
   * @param type one of TYPE_...
   * @throws IOException if unsupported TYPE_...
   */
  public AnalyzeInfo(String type) throws IOException {
    if (!type.equals(TYPE_METHOD)) {
      throw new IOException("unsupported type '" + type + "'");
    }
    this.type = type;
  }
  
  
  /**
   * Creates an AnalyzeInfo from a method element.
   * Used during apt processing.
   *
   * @param processingEnv the annotation processor's environment
   * @param methodElement the ExecutableElement to create an info from
   * @throws java.io.IOException if some I/O error occurs
   */
  public AnalyzeInfo(ProcessingEnvironment processingEnv, ExecutableElement methodElement) throws IOException {
    this(TYPE_METHOD);
    Element classElement = methodElement.getEnclosingElement();
    setClassName(classElement.toString());
    try {
      setSuperClassName(((TypeElement)classElement).getSuperclass().toString());
    }
    catch (Exception ex) {
      setSuperClassName(null);    // no superclass
    }
    setMethodName(methodElement.getSimpleName().toString());
    TypeMirror tm = methodElement.getReturnType();
    setReturnType(tm.toString());

    setReturningAppDbObject(isTypeInstanceoOf(processingEnv, tm, CLASSNAME_APPDBOBJECT));
    setReturningAppDbObjectCollection(isTypeInstanceoOf(processingEnv, tm, CLASSNAME_COLLECTION, CLASSNAME_APPDBOBJECT));
    setReturningAppDbCursor(isTypeInstanceoOf(processingEnv, tm, CLASSNAME_DBCURSOR, CLASSNAME_APPDBOBJECT));
    
    if (isReturningAppDbObject()) {
      setReturningDbObject(true);
    }
    else  {
      setReturningDbObject(isTypeInstanceoOf(processingEnv, tm, CLASSNAME_DBOBJECT));
    }
    if (isReturningAppDbObjectCollection()) {
      setReturningDbObjectCollection(true);
    }
    else  {
      setReturningDbObjectCollection(isTypeInstanceoOf(processingEnv, tm, CLASSNAME_COLLECTION, CLASSNAME_DBOBJECT));
    }
    if (isReturningAppDbCursor()) {
      setReturningDbCursor(true);
    }
    else  {
      setReturningDbCursor(isTypeInstanceoOf(processingEnv, tm, CLASSNAME_DBCURSOR));
    }

    setModifiers(methodElement.getModifiers().toArray(new Modifier[0]));
    varArgsMethod = methodElement.isVarArgs();
    List<? extends VariableElement> varList = methodElement.getParameters();
    for (int i=0; i < varList.size(); i++) {
      Parameter par = new Parameter(processingEnv, varList.get(i));
      if (i == varList.size()-1) {
        par.varArg = varArgsMethod;
      }
      addParameter(par);
    }    
  }
  
  
  /**
   * Gets the type of this analyze info.
   * 
   * @return the type
   */
  public String getType() {
    return type;
  }

  
  /**
   * Gets the name of the class this analyze info is part of.
   * 
   * @return the classname
   */
  public String getClassName() {
    return className;
  }

  /**
   * Sets the classname and the packagename from a given classname.
   * 
   * @param className the full class name
   */
  public void setClassName(String className) {
    this.className = className;
    // compile pattern
    String packageName = "";
    int ndx = className.lastIndexOf('.');
    if (ndx > 0)  {
      packageName = className.substring(0, ndx + 1);
      // replace all dots with \\.
      StringBuilder buf = new StringBuilder();
      for (int i=0; i < packageName.length(); i++) {
        char c = packageName.charAt(i);
        if (c == '.') {
          buf.append("\\.");
        }
        else  {
          buf.append(c);
        }
      }
      packageName = buf.toString();
    }
    package_Pattern = Pattern.compile(packageName);
  }
  
  
  /**
   * Gets the name of the superclass this analyze info is part of.
   * 
   * @return the classname
   */
  public String getSuperClassName() {
    return superClassName;
  }

  /**
   * Sets the classname of the superclass.
   * 
   * @param superClassName the full class name
   */
  public void setSuperClassName(String superClassName) {
    this.superClassName = superClassName;
  }

  
  /**
   * Gets the method name the annotation belongs to.
   * 
   * @return the method name
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Sets the method name the annotation belongs to.
   * 
   * @param methodName the method name
   */
  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  
  /**
   * Gets the return type of the method
   * @return the return type
   */
  public String getReturnType() {
    return returnType;
  }

  /**
   * Sets the return type of the method
   * @param returnType the return type
   */
  public void setReturnType(String returnType) {
    this.returnType = returnType;
  }


  /**
   * Gets the formal parameters of the method.
   * 
   * @return the array of parameters
   */
  public Parameter[] getParameters() {
    return parameterList == null ? null : parameterList.toArray(new Parameter[parameterList.size()]);
  }
  
  /**
   * Adds a formal parameter
   * @param parameter the formal parameter to add
   */
  public void addParameter(Parameter parameter) {
    if (parameterList == null) {
      parameterList = new ArrayList<Parameter>();
    }
    parameterList.add(parameter);
  }

  
  /**
   * Returns whether the method has varargs.
   * @return true if varargs method
   */
  public boolean isVarArgsMethod() {
    return varArgsMethod;
  }

  /**
   * Sets whether the method has varargs.
   * @param varArgsMethod true if varargs method
   */
  public void setVarArgsMethod(boolean varArgsMethod) {
    this.varArgsMethod = varArgsMethod;
  }

  /**
   * Gets the modifiers for the method
   * @return the array of modifiers
   */
  public Modifier[] getModifiers() {
    return modifiers;
  }
  
  /**
   * Gets the method's modifiers as a string
   * @return the modifiers as a string
   */
  public String getModifiersAsString() {
    return objectArrayToString(modifiers, " ");
  }
  
  /**
   * Checks whether given modifier is set for the method
   * @param modifier the modifier to test
   * @return true if modifier set
   */
  public boolean isModifierSet(Modifier modifier) {
    return isModifierSet(modifier, modifiers);
  }
  
  /**
   * Sets the modifiers for the method
   * @param modifiers for the method
   */
  public void setModifiers(Modifier[] modifiers) {
    this.modifiers = modifiers;
  }
  
  /**
   * Checks whether the method is returning a {@link org.tentackle.db.DbObject}.
   * @return true if method returns a DbObject.
   */
  public boolean isReturningDbObject() {
    return returningDbObject;
  }

  /**
   * Sets whether the method is returning a {@link org.tentackle.db.DbObject}.
   * @param returningDbObject true if method returns a DbObject.
   */
  public void setReturningDbObject(boolean returningDbObject) {
    this.returningDbObject = returningDbObject;
  }

  /**
   * Checks whether the method is returning a collection of {@link org.tentackle.db.DbObject}s.
   * @return true if method returns a collection of DbObjects.
   */
  public boolean isReturningDbObjectCollection() {
    return returningDbObjectCollection;
  }

  /**
   * Sets whether the method is returning a collection of {@link org.tentackle.db.DbObject}s.
   * @param returningDbObjectCollection true if method returns a collection of DbObjects.
   */
  public void setReturningDbObjectCollection(boolean returningDbObjectCollection) {
    this.returningDbObjectCollection = returningDbObjectCollection;
  }

  /**
   * Checks whether the method is returning a {@link org.tentackle.db.DbCursor}s.
   * @return true if method returns a collection of DbCursor.
   */
  public boolean isReturningDbCursor() {
    return returningDbCursor;
  }

  /**
   * Sets whether the method is returning a {@link org.tentackle.db.DbCursor}.
   * @param returningDbCursor true if method returns a collection of DbCursor.
   */
  public void setReturningDbCursor(boolean returningDbCursor) {
    this.returningDbCursor = returningDbCursor;
  }
  
  /**
   * Checks whether the method is returning an {@link org.tentackle.appworx.AppDbObject}.
   * @return true if method returns an AppDbObject.
   */
  public boolean isReturningAppDbObject() {
    return returningAppDbObject;
  }

  /**
   * Sets whether the method is returning an {@link org.tentackle.appworx.AppDbObject}.
   * @param returningAppDbObject true if method returns an AppDbObject.
   */
  public void setReturningAppDbObject(boolean returningAppDbObject) {
    this.returningAppDbObject = returningAppDbObject;
  }

  /**
   * Checks whether the method is returning a collection of
   * {@link org.tentackle.appworx.AppDbObject}s.
   * 
   * @return true if method returns a collection of AppDbObjects.
   */
  public boolean isReturningAppDbObjectCollection() {
    return returningAppDbObjectCollection;
  }

  /**
   * Sets whether the method is returning a collection of
   * {@link org.tentackle.appworx.AppDbObject}s.
   * 
   * @param returningAppDbObjectCollection true if method returns a collection of AppDbObjects.
   */
  public void setReturningAppDbObjectCollection(boolean returningAppDbObjectCollection) {
    this.returningAppDbObjectCollection = returningAppDbObjectCollection;
  }

  /**
   * Checks whether the method is returning an
   * {@link org.tentackle.appworx.AppDbCursor}.
   * 
   * @return true if method returns an AppDbCursor.
   */
  public boolean isReturningAppDbCursor() {
    return returningAppDbCursor;
  }

  /**
   * Sets whether the method is returning an
   * {@link org.tentackle.appworx.AppDbCursor}.
   * 
   * @param returningAppDbCursor true if method returns an AppDbCursor.
   */
  public void setReturningAppDbCursor(boolean returningAppDbCursor) {
    this.returningAppDbCursor = returningAppDbCursor;
  }

  
  
  
  
  /**
   * Gets the declaration string
   */
  @Override
  public String toString() {
    if (type.equals(TYPE_METHOD)) {
      return getModifiersAsString() + " " + getReturnType() + " " + getMethodName() + "(" + objectArrayToString(getParameters(), ", ") + ")";
    }
    return null;
  }
  
  
  /**
   * Writes this object to an info file.
   *
   * @param writer is the PrintWriter object
   * @throws IOException if write failed
   */
  public void write(PrintWriter writer) throws IOException {
    if (type.equals(TYPE_METHOD)) {
      writer.println("# AnalyzeInfo Version " + INFO_FILE_VERSION);
      writer.println("# " + this);
      writer.println(TYPE_METHOD + ": " + methodName);
      writer.println(TYPE_CLASS + ": " + className);
      if (superClassName != null) {
        writer.println("superclass: " + superClassName);
      }
      writer.println("returns: " + returnType +
                     " " + Boolean.toString(returningDbObject) +
                     " " + Boolean.toString(returningDbObjectCollection) +
                     " " + Boolean.toString(returningDbCursor) +
                     " " + Boolean.toString(returningAppDbObject) +
                     " " + Boolean.toString(returningAppDbObjectCollection) +
                     " " + Boolean.toString(returningAppDbCursor));
      writer.println("modifiers: " + getModifiersAsString());
      writer.println("varargs: " + varArgsMethod);
      if (parameterList != null) {
        int argc = 0;
        for (Parameter par: parameterList) {
          writer.print("arg[" + argc++ + "]: ");
          par.write(writer);
          writer.println();
        }
      }
    }
    else {
      throw new IOException("unkown info type: " + type);
    }
  }
  
  
  /**
   * Reads info from an file.
   *
   * @param infoFile the file to read from
   * @return an AnalyzeInfo object, null if file is empty or does not contain an AnalyzeInfo-text
   * @throws FileNotFoundException
   * @throws IOException 
   */  
  public static AnalyzeInfo readInfo(File infoFile) throws FileNotFoundException, IOException {
    LineNumberReader reader = new LineNumberReader(new FileReader(infoFile));
    AnalyzeInfo info = readInfo(reader);
    reader.close();
    return info;
  }
  
  
  /**
   * Reads info from an line reader.
   *
   * @param reader is the LineNumberReader
   * @return an AnalyzeInfo object, null if file is empty or does not contain an AnalyzeInfo-text
   * @throws IOException 
   */
  public static AnalyzeInfo readInfo(LineNumberReader reader) throws IOException {
    AnalyzeInfo info = null;
    String line;
    while((line = reader.readLine()) != null) {
      if (!line.startsWith("#")) {    // skip comment lines
        StringTokenizer stok = new StringTokenizer(line, ":");
        if (stok.hasMoreTokens()) {
          String type = stok.nextToken();
          if (info == null) {
            // first line defines the info type
            info = new AnalyzeInfo(type);
          }
          if (stok.hasMoreTokens()) {
            String text = stok.nextToken();
            stok = new StringTokenizer(text);
            // process according to line type
            if (type.equals(TYPE_METHOD)) {
              info.setMethodName(stok.nextToken());
            }
            else if (type.equals(TYPE_CLASS)) {
              info.setClassName(stok.nextToken());
            }
            else if (type.equals("superclass")) {
              info.setSuperClassName(stok.nextToken());
            }
            else if (type.equals("returns")) {
              info.setReturnType(stok.nextToken());
              info.setReturningDbObject(Boolean.valueOf(stok.nextToken()));
              info.setReturningDbObjectCollection(Boolean.valueOf(stok.nextToken()));
              info.setReturningDbCursor(Boolean.valueOf(stok.nextToken()));
              info.setReturningAppDbObject(Boolean.valueOf(stok.nextToken()));
              info.setReturningAppDbObjectCollection(Boolean.valueOf(stok.nextToken()));
              info.setReturningAppDbCursor(Boolean.valueOf(stok.nextToken()));
            }
            else if (type.equals("modifiers")) {
              info.setModifiers(readModifiers(stok));
            }
            else if (type.equals("varargs")) {
              info.setVarArgsMethod(Boolean.valueOf(stok.nextToken()));
            }
            else if (type.startsWith("arg")) {
              info.addParameter(new Parameter(stok));
            }
          }
        }
      }
    }
    
    if (info != null) {
      if (info.getType().equals(TYPE_METHOD)) {
        // set vararg in the last parameter
        if (info.isVarArgsMethod()) {
          info.parameterList.get(info.parameterList.size()-1).varArg = true;
        }
      }
    }
    
    return info;
  }
  
  
  
  /**
   * Gets an array of modifiers from a string tokenizer
   */
  private static Modifier[] readModifiers(StringTokenizer stok) {
    List<Modifier> modList = new ArrayList<Modifier>();
    while (stok.hasMoreTokens()) {
      try {
        modList.add(Modifier.valueOf(stok.nextToken().toUpperCase()));
      }
      catch (IllegalArgumentException ex) {
        // unknown modifier: ignore
      }
    }
    return modList.toArray(new Modifier[modList.size()]);
  } 
  
  
  
  

  /**
   * Creates a string from an object array.
   * Copied from StringHelper to keep dependencies small.
   *
   * @param objArray the array of objects
   * @param separator the string between two objects
   */
  private static String objectArrayToString(Object[] objArray, String separator) {
    StringBuilder buf = new StringBuilder();
    if (objArray != null) {
      boolean addSeparator = false;
      for (Object obj: objArray) {
        if (addSeparator) {
          buf.append(separator);
        }
        buf.append(obj.toString());
        addSeparator = true;
      }
    }
    return buf.toString();
  }
    

  /**
   * Checks if a modifier is in the set of modifiers
   * @param modifier the modifier to check
   * @param modifierList the list of modifiers
   * @return true if modifier is in the list of modifiers
   */
  public static boolean isModifierSet(Modifier modifier, Modifier[] modifierList) {
    boolean rv = false;
    if (modifierList != null) {
      for (Modifier m: modifierList) {
        if (m.equals(modifier)) {
          rv = true;
          break;
        }
      }
    }
    return rv;
  }
  
  
  // remove these strings
  private static final Pattern java_lang_Pattern              = Pattern.compile("java\\.lang\\.");
  private static final Pattern org_tentackle_db_Pattern       = Pattern.compile("org\\.tentackle\\.db\\.");
  private static final Pattern org_tentackle_appworx_Pattern  = Pattern.compile("org\\.tentackle\\.appworx\\.");
  private static final Pattern java_util_Pattern              = Pattern.compile("java\\.util\\.");
  private static final Pattern org_tentackle_util_Pattern     = Pattern.compile("org\\.tentackle\\.util\\.");
  
  // cut off package name
  private Pattern package_Pattern;
  
  
  /**
   * Simplifies some classnames by removing package names.
   * Makes generated code better readable.
   * Can be used by wurblets.
   * @param type the full type
   * @return the shortened type
   */
  public String cleanTypeString(String type) {
    // cut out leading package names
    type = java_lang_Pattern.matcher(type).replaceAll("");
    type = org_tentackle_db_Pattern.matcher(type).replaceAll("");
    type = org_tentackle_appworx_Pattern.matcher(type).replaceAll("");
    type = java_util_Pattern.matcher(type).replaceAll("");
    type = org_tentackle_util_Pattern.matcher(type).replaceAll("");
    
    // cut out current package
    type = package_Pattern.matcher(type).replaceAll("");
    
    return type;
  }


  // checks whether a type is an instanceof of a given classname and optional generics type
  private static boolean isTypeInstanceoOf(ProcessingEnvironment processingEnv, TypeMirror typeMirror, String className, String genType) {
    
    String typeName;
    
    boolean genTypeOk = true;
    
    if (typeMirror instanceof DeclaredType) {
      typeName = ((DeclaredType)typeMirror).asElement().toString();
      if (genType != null) {
        // check type paramaters
        genTypeOk = false;
        for (TypeMirror tm: ((DeclaredType)typeMirror).getTypeArguments()) {
          if (isTypeInstanceoOf(processingEnv, tm, genType)) {
            genTypeOk = true;
            break;
          } 
        }
      }
    }
    else  {
      typeName = typeMirror.toString();
    }
    
    if (typeName.equals(className)) {
      // true if optional type parameters and the classname matches 
      return genTypeOk;
    }
    
    for (TypeMirror tm: processingEnv.getTypeUtils().directSupertypes(typeMirror)) {
      if (isTypeInstanceoOf(processingEnv, tm, className, genType)) {
        return true;
      }
    }
    return false;
  }
  
  private static boolean isTypeInstanceoOf(ProcessingEnvironment processingEnv, TypeMirror typeMirror, String className) {
    return isTypeInstanceoOf(processingEnv, typeMirror, className, null);
  }


}
