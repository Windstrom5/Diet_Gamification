package com.example.diet_gamification.utils

import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.DriverManager

fun connectToPostgres() {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            // Load the PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver")

            // Use your ngrok URL as the host
            val url = "jdbc:postgresql://selected-jaguar-presently.ngrok-free.app:5432/Diet_Gamification"
            val user = "your_username"
            val password = "your_password"

            val connection: Connection = DriverManager.getConnection(url, user, password)

            // Example query
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery("SELECT * FROM your_table")

            while (resultSet.next()) {
                val columnData = resultSet.getString("your_column")
                println("Data: $columnData")
            }

            resultSet.close()
            statement.close()
            connection.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

