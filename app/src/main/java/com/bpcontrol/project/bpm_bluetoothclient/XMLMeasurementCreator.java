package com.bpcontrol.project.bpm_bluetoothclient;


import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.StringWriter;


/**
 * Created by Adrian on 15/3/15.
 */
public class XMLMeasurementCreator {

    private String systolicPressure;
    private String diastolicPressure;
    private String pulse;

    public XMLMeasurementCreator(String systolicPressure,String diastolicPressure,String pulse) {

        this.systolicPressure = systolicPressure;
        this.diastolicPressure = diastolicPressure;
        this.pulse = pulse;

    }

    public String createXML(){

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        try {

            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "measurement");

            serializer.startTag("","systolic-pressure");
            serializer.text(systolicPressure);
            serializer.endTag("","systolic-pressure");

            serializer.startTag("","diastolic-pressure");
            serializer.text(diastolicPressure);
            serializer.endTag("","diastolic-pressure");

            serializer.startTag("","pulse");
            serializer.text(pulse);
            serializer.endTag("","pulse");

            serializer.endTag("", "measurement");
            serializer.endDocument();
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public String getSystolicPressure() {
        return systolicPressure;
    }

    public String getDiastolicPressure() {
        return diastolicPressure;
    }

    public String getPulse() {
        return pulse;
    }
}
