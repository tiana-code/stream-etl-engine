package com.streametl.window;

import com.streametl.model.Record;

import java.util.List;

public sealed interface WindowFunction permits TumblingWindow, SlidingWindow, SessionWindow {

    List<List<Record>> apply(List<Record> records);

    String name();
}
