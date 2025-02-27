package com.github.espressopad.views.components;

import javax.swing.JTextField;
import javax.swing.text.Document;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class PlaceHolderTextField extends JTextField {
    private String placeHolder;

    public PlaceHolderTextField() {
    }

    public PlaceHolderTextField(Document pDoc, String pText, int pColumns) {
        super(pDoc, pText, pColumns);
    }

    public PlaceHolderTextField(int pColumns) {
        super(pColumns);
    }

    public PlaceHolderTextField(String pText) {
        super(pText);
    }

    public PlaceHolderTextField(String pText, int pColumns) {
        super(pText, pColumns);
    }

    public String getPlaceHolder() {
        return this.placeHolder;
    }

    public void setPlaceHolder(String s) {
        this.placeHolder = s;
    }

    @Override
    protected void paintComponent(Graphics pG) {
        super.paintComponent(pG);

        if (this.placeHolder == null || this.placeHolder.isEmpty() || !this.getText().isEmpty()) return;

        Graphics2D g = (Graphics2D) pG;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(this.getDisabledTextColor());
        g.drawString(this.placeHolder, this.getInsets().left, pG.getFontMetrics().getMaxAscent() + this.getInsets().top);
    }
}
