package week11.st695922.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import week11.st695922.finalproject.ui.GoSpotApp
import week11.st695922.finalproject.ui.theme.GOSpotTheme
import week11.st695922.finalproject.worker.scheduleDailyStationReset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleDailyStationReset(applicationContext)
        setContent {
            GOSpotTheme {
                GoSpotApp()
            }
        }
    }
}
