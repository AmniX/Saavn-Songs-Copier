package com.example.aman.saavnfucker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Aman on 10-10-2016.
 */

public class LogsTextView extends TextView {
    public LogsTextView(Context context) {
        super(context);
    }

    public LogsTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LogsTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void appendLine(String text) {
        append("\n" + text);
    }

}
