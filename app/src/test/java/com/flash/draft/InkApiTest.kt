package com.flash.draft

import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.Stroke
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import java.lang.reflect.Modifier

import org.junit.Test

class InkApiTest {
    @Test
    fun dumpInkApi() {
    println("--- MutableStrokeInputBatch Methods ---")
    MutableStrokeInputBatch::class.java.methods.forEach {
        println("${it.name} - ${it.parameterTypes.map { p -> p.name }.joinToString()}")
    }
    
    println("\n--- StrokeInput Methods ---")
    StrokeInput::class.java.methods.forEach {
        println("${it.name} - ${it.parameterTypes.map { p -> p.simpleName }.joinToString()}")
    }
    
    println("\n--- Brush Methods ---")
    Brush::class.java.methods.forEach {
        println("${it.name} - ${it.parameterTypes.map { p -> p.simpleName }.joinToString()}")
    }
    
    println("\n--- Stroke Constructors ---")
    Stroke::class.java.constructors.forEach {
        println("${it.name} - ${it.parameterTypes.map { p -> p.simpleName }.joinToString()}")
    }
}
}
