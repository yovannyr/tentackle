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

// $Id: FormTelnoField.java 336 2008-05-09 14:40:20Z harald $

package org.tentackle.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.StringTokenizer;
import org.tentackle.plaf.PlafGlobal;
import org.tentackle.util.Telno;


/**
 * A field to edit phone numbers and fax, mobile, etc... numbers as well.
 * There can be more than one number per field, separated by commas.
 * Comments (i.e. non-number text) are allowed and parsed too.
 * Characters between square brackets will be ignored (those are errormessages).
 * <pre>
 * Example:
 * "+49 7722 9508-0 work, +49 172 7364559 mobile"
 *
 * @author harald
 */

public class FormTelnoField extends FormFieldComponentPanel {

  // the number type
  
  /** number type for  **/
  public static final char  UNKNOWN = '?';    // could be anything
  /** number type for  **/
  public static final char  PHONE   = 'P';    // phone numbers only
  /** number type for  **/
  public static final char  FAX     = 'F';    // fax numbers only
  /** number type for  **/
  public static final char  CELL    = 'C';    // cell (mobile) phone
  /** number type for  **/
  public static final char  MODEM   = 'M';    // modem

  
  private TelnoField telnoField;                  // the formfield
  private int defaultCountry;                     // default country code, 0 = none
  private char telnoType;                         // type (see above)
  private int telnoNum;                           // number of telnos in string
  private Telno[]  telnos;                        // numbers
  private String[] comments;                      // comments for numbers
  private String[] errors;                        // errors from parsing
  private String[] unparsed;                      // unparsed numbers
  
  
  private FormButton telnoButton = new FormButton();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();


  /**
   * Creates a phone field.
   */
  public FormTelnoField() {
    telnoField = new TelnoField();
    setFormFieldComponent(telnoField);
    initComponents();
    setTelnoType(UNKNOWN);
  }

  
  /**
   * Sets the telno type.
   * 
   * @param telnoType the phone number type, one of <tt>PHONE,FAX,CELL or MODEM</tt>
   */
  public void setTelnoType (char telnoType) {
    this.telnoType = telnoType;
    String iconName = "file";
    switch (telnoType) {
      case PHONE:   iconName = "phone";     break;
      case FAX:     iconName = "fax";       break;
      case CELL:    iconName = "cellphone"; break;
      case MODEM:   iconName = "modem";     break;
    }
    telnoButton.setIcon(PlafGlobal.getIcon(iconName));
  }

  /**
   * Gets the telno type.
   * 
   * @return the phone number type, one of <tt>PHONE,FAX,CELL or MODEM</tt>
   */
  public char getTelnoType ()  {
    return this.telnoType;
  }

  

  public int getDefaultCountry() {
    return defaultCountry;
  }
  
  /**
   * Sets the default country code.
   * Will be prepended to the numbers, if missing.
   * @param defaultCountry the default country code.
   * @see Telno#setDefaultCountry(int) 
   */
  public void setDefaultCountry(int defaultCountry) {
    this.defaultCountry = defaultCountry;
  }
  
  


  /**
   * Gets the number of phone numbers.
   * 
   * @return the number of numbers
   */
  public int getTelnoNum() {
    return telnoNum;
  }

  
  /**
   * Gets the parsed numbers
   * @return the array of phone numbers
   */
  public Telno[] getTelnos() {
    return telnos;
  }

  /**
   * Gets a single number.
   * 
   * @param ndx the index within the array of numbers
   * @return the number
   */
  public Telno getTelno (int ndx)  {
    return telnos[ndx];
  }

  
  /**
   * Gets the parsed comments (one for each number)
   * @return the array of comments
   */
  public String[] getComments() {
    return comments;
  }

  /**
   * Gets the comment for a singe number
   * @param ndx the within the array of numbers
   * @return the comment
   */
  public String getComment (int ndx)  {
    return comments[ndx];
  }



  /**
   * Parse phone-numbers.
   * Numbers are separated by commas.
   * Any character other than a digit and + will be treated as comment
   * Chars between [ and ] will be ignored (usually errormessages)
   * 
   * @return number of errors
   */
  private int parseTelno(String tel) {
    int errorCount = 0;
    StringTokenizer tk = null;
    
    if (tel == null)  {
      telnoNum = 0;
    }
    else  {
      tk = new StringTokenizer (tel, ",");
      telnoNum = tk.countTokens();
    }
    
    if (telnoNum < 1)  {
      telnos   = null;
      comments = null;
      errors   = null;
      unparsed = null;
    }
    else  {
      telnos   = new Telno[telnoNum];
      comments = new String[telnoNum];
      errors   = new String[telnoNum];
      unparsed = new String[telnoNum];

      int ndx = 0;
      char c;
      while (tk.hasMoreTokens())  {
        String token = tk.nextToken();
        int len = token.length();
        StringBuilder telno = new StringBuilder(len);
        StringBuilder comment = new StringBuilder(len);
        boolean inTelno = true;
        boolean inError = false;
        for (int i=0; i < len; i++) {
          c = token.charAt(i);
          if (c == ']') {
            inError = false;
            continue;
          }
          if (c == '[' || inError) {
            inError = true;
            continue;   // skip
          }
          if (inTelno &&
              Character.isDigit(c) == false && c != '+' && c != '-' && c != ' ') {
            inTelno = false;
          }
          if (inTelno) {
            telno.append(c);
          }
          else {
            comment.append(c);
          }
        }
        unparsed[ndx] = telno.toString().trim();
        telnos[ndx]   = new Telno();
        telnos[ndx].setDefaultCountry(defaultCountry);
        comments[ndx] = comment.toString().trim();
        if (telnos[ndx].setTelno(unparsed[ndx]) == false) {
          errors[ndx] = telnos[ndx].getError();
          errorCount++;
        }
        ndx++;
      }
    }
    return errorCount;
  }

  
  
  

  private void initComponents() {
    telnoButton.setMargin(new Insets(0, 0, 0, 0));
    this.setLayout(gridBagLayout1);
    telnoButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        telnoButton_actionPerformed(e);
      }
    });
    this.add(telnoField, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 1), 0, 0));
    this.add(telnoButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  private void telnoButton_actionPerformed(ActionEvent e) {
    if (isChangeable() || (getText() != null && getText().length() > 0))  {
      // startet den Telno-Dialog: wählen bzw. bearbeiten
      String newTelno = FormTelnoDialog.createFormTelnoDialog(this).showDialog();
      if (isChangeable() && newTelno != null) {
        setText(newTelno);
        fireValueEntered();
      }
    }
  }

  
  
  
  
  
  /**
   * special Stringformfield that parses numbers and sets error 
   */
  
  private class TelnoField extends StringFormField {
    
    @Override
    public String getText () {
      parseTelno(super.getText());          // parse numbers and comments
      StringBuilder buf = new StringBuilder();
      int errorNdx = -1;
      for (int i=0; i < telnoNum; i++)  {
        if (i > 0) {
          buf.append(", ");
        }
        if (errors[i] != null)  {
          buf.append(unparsed[i]);
          buf.append('[');
          buf.append(errors[i]);
          buf.append(']');
          if (errorNdx == -1) {
            errorNdx = i;
          }
        }
        else  {
          buf.append(telnos[i].getTelno());
        }
        if (comments[i].length() > 0) {
          buf.append(' ');
          buf.append(comments[i]);
        }
      }
      String tel = buf.toString();
      if (errorNdx >= 0)  {
        super.setText(tel);   // show errors
        errorOffset = 0;
        for (int i=0; i < errorNdx; i++) {
          errorOffset = tel.indexOf(',', errorOffset) + 1;
        }
      }
      return tel;
    }
  }
  
}