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

// $Id: AnalyzeProcessor.java 336 2008-05-09 14:40:20Z harald $


package org.tentackle.annotations;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import static javax.lang.model.SourceVersion.*;
        

/**
 * Annotation Processor for the Analyze-annotation.
 * 
 * @author harald
 */
@SupportedAnnotationTypes("org.tentackle.annotations.Analyze")
@SupportedSourceVersion(RELEASE_6)
public class AnalyzeProcessor extends AbstractProcessor {
  
  /**
   * Creates the annotation processor for the {@link Analyze} annotation.
   */
  public AnalyzeProcessor() {
    super();
  }

  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!roundEnv.processingOver()) {
      for (TypeElement te: annotations) {
        for (Element e: roundEnv.getElementsAnnotatedWith(te)) {
          if (e.getKind().equals(ElementKind.METHOD)) {
              ExecutableElement methodElement = (ExecutableElement)e;
              Analyze ng = methodElement.getAnnotation(Analyze.class);
              CharSequence methodName = ng.value().startsWith("[") ? methodElement.getSimpleName() : ng.value();
              try {
                // create info object
                AnalyzeInfo info = new AnalyzeInfo(processingEnv, methodElement);
                
                // write method information to auxiliary file
                // SOURCE_OUTPUT is destdir= in javac ant-task and should point to
                // a separate directory than the source!
                FileObject f = processingEnv.getFiler().createResource(
                                        StandardLocation.SOURCE_OUTPUT, 
                                        info.getClassName(), 
                                        methodName + AnalyzeInfo.INFO_FILE_EXTENSION);
                PrintWriter pw = new PrintWriter(new BufferedWriter(f.openWriter()));
                info.write(pw);
                pw.close();
              } 
              catch (Exception ex) {
                ex.printStackTrace();
              }
          }
          else  {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "annotated element is not a method", e);
          }
        }
      }
    }
    return true; // claim annotation
  }
  
}
