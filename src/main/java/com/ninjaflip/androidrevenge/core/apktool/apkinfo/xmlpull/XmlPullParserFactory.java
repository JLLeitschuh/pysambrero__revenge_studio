package com.ninjaflip.androidrevenge.core.apktool.apkinfo.xmlpull;

import com.ninjaflip.androidrevenge.exceptions.XmlPullParserException;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


public class XmlPullParserFactory {
    static final Class referenceContextClass;
    public static final String PROPERTY_NAME = "XmlPullParserFactory";
    private static final String RESOURCE_NAME = "/META-INF/services/XmlPullParserFactory";
    protected Vector parserClasses;
    protected String classNamesLocation;
    protected Vector serializerClasses;

    static {
        XmlPullParserFactory f = new XmlPullParserFactory();
        referenceContextClass = f.getClass();
    }


    protected Hashtable features = new Hashtable();


    protected XmlPullParserFactory() {
    }


    public void setFeature(String name, boolean state)
            throws XmlPullParserException {
        features.put(name, new Boolean(state));
    }


    public boolean getFeature(String name) {
        Boolean value = (Boolean) features.get(name);
        return value != null ? value.booleanValue() : false;
    }


    public void setNamespaceAware(boolean awareness) {
        features.put("http://xmlpull.org/v1/doc/features.html#process-namespaces", new Boolean(awareness));
    }


    public boolean isNamespaceAware() {
        return getFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces");
    }


    public void setValidating(boolean validating) {
        features.put("http://xmlpull.org/v1/doc/features.html#validation", new Boolean(validating));
    }


    public boolean isValidating() {
        return getFeature("http://xmlpull.org/v1/doc/features.html#validation");
    }


    public XmlPullParser newPullParser()
            throws XmlPullParserException {
        if (parserClasses == null) {
            throw new XmlPullParserException("Factory initialization was incomplete - has not tried " + classNamesLocation);
        }
        if (parserClasses.size() == 0) {
            throw new XmlPullParserException("No valid parser classes found in " + classNamesLocation);
        }
        StringBuffer issues = new StringBuffer();

        for (int i = 0; i < parserClasses.size(); i++) {
            Class ppClass = (Class) parserClasses.elementAt(i);
            try {
                XmlPullParser pp = (XmlPullParser) ppClass.newInstance();


                for (Enumeration e = features.keys(); e.hasMoreElements(); ) {
                    String key = (String) e.nextElement();
                    Boolean value = (Boolean) features.get(key);
                    if ((value != null) && (value.booleanValue())) {
                        pp.setFeature(key, true);
                    }
                }
                return pp;
            } catch (Exception ex) {
                issues.append(ppClass.getName() + ": " + ex.toString() + "; ");
            }
        }

        throw new XmlPullParserException("could not create parser: " + issues);
    }


    public XmlSerializer newSerializer()
            throws XmlPullParserException {
        if (serializerClasses == null) {
            throw new XmlPullParserException(
                    "Factory initialization incomplete - has not tried " + classNamesLocation);
        }
        if (serializerClasses.size() == 0) {
            throw new XmlPullParserException(
                    "No valid serializer classes found in " + classNamesLocation);
        }

        StringBuffer issues = new StringBuffer();

        for (int i = 0; i < serializerClasses.size(); i++) {
            Class ppClass = (Class) serializerClasses.elementAt(i);
            try {
                return (XmlSerializer) ppClass.newInstance();


            } catch (Exception ex) {


                issues.append(ppClass.getName() + ": " + ex.toString() + "; ");
            }
        }

        throw new XmlPullParserException("could not create serializer: " + issues);
    }


    public static XmlPullParserFactory newInstance()
            throws XmlPullParserException {
        return newInstance(null, null);
    }

    public static XmlPullParserFactory newInstance(String classNames, Class context)
            throws XmlPullParserException {
        if (context == null) {


            context = referenceContextClass;
        }

        String classNamesLocation = null;

        if ((classNames == null) || (classNames.length() == 0) || ("DEFAULT".equals(classNames))) {
            try {
                InputStream is = context.getResourceAsStream("/META-INF/services/XmlPullParserFactory");

                if (is == null) {
                    throw new XmlPullParserException("resource not found: /META-INF/services/XmlPullParserFactory make sure that parser implementing XmlPull API is available");
                }
                StringBuffer sb = new StringBuffer();
                for (; ; ) {
                    int ch = is.read();
                    if (ch < 0) break;
                    if (ch > 32)
                        sb.append((char) ch);
                }
                is.close();

                classNames = sb.toString();
            } catch (Exception e) {
                throw new XmlPullParserException(null, null, e);
            }
            classNamesLocation = "resource /META-INF/services/XmlPullParserFactory that contained '" + classNames + "'";
        } else {
            classNamesLocation =
                    "parameter classNames to newInstance() that contained '" + classNames + "'";
        }

        XmlPullParserFactory factory = null;
        Vector parserClasses = new Vector();
        Vector serializerClasses = new Vector();
        int pos = 0;

        while (pos < classNames.length()) {
            int cut = classNames.indexOf(',', pos);

            if (cut == -1) cut = classNames.length();
            String name = classNames.substring(pos, cut);

            Class candidate = null;
            Object instance = null;
            try {
                candidate = Class.forName(name);

                instance = candidate.newInstance();
            } catch (Exception localException1) {
            }

            if (candidate != null) {
                boolean recognized = false;
                if ((instance instanceof XmlPullParser)) {
                    parserClasses.addElement(candidate);
                    recognized = true;
                }
                if ((instance instanceof XmlSerializer)) {
                    serializerClasses.addElement(candidate);
                    recognized = true;
                }
                if ((instance instanceof XmlPullParserFactory)) {
                    if (factory == null) {
                        factory = (XmlPullParserFactory) instance;
                    }
                    recognized = true;
                }
                if (!recognized) {
                    throw new XmlPullParserException("incompatible class: " + name);
                }
            }
            pos = cut + 1;
        }

        if (factory == null) {
            factory = new XmlPullParserFactory();
        }
        parserClasses = parserClasses;
        serializerClasses = serializerClasses;
        classNamesLocation = classNamesLocation;
        return factory;
    }
}
