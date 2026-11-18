package com.bamboomosaic.llmchat

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bamboomosaic.llmchat.feature.chat.ChatScreen

object AppDestinations {
    const val CHAT = "chat"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppDestinations.CHAT) {
        composable(AppDestinations.CHAT) {
            ChatScreen()
        }
    }
}
