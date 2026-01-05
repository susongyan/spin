package com.zuomagai.spin.sqlir.emit;

import com.zuomagai.spin.sqlir.Statement;

public interface SqlEmitter {
    String emit(Statement statement);
}
