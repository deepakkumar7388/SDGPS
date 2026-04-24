package com.example.digitalpass

data class BatchData(
    var token:String,
    var batchName:String,
    var level1:ArrayList<String>,
    var level2:ArrayList<String>
)