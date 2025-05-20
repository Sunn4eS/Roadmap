package com.example.roadMap.activities

import android.content.Intent
import android.os.Bundle
import android.view.Display
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadMap.MainActivity
import com.example.roadMap.data.dataBase.AppDatabase
import com.example.roadMap.data.dataBase.UserEntity
import com.example.roadMap.data.utilities.comparePassword
import com.example.roadMap.data.utilities.hashPassword
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WelcomeScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WelcomeScreen()
        }

    }
}
@Composable
fun WelcomeScreen() {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Добро пожаловать!",
            fontSize = 24.sp
        )
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Введите ваше имя: ") },
            modifier = Modifier
                .fillMaxWidth(),

            )
        Spacer(
            modifier = Modifier
                .height(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Введите пароль: ") },
            modifier = Modifier
                .fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier
                .height(18.dp)
        )

        Spacer(modifier = Modifier.height(15.dp))

        Row () {
            LoginButton(name, password);
            RegisterButton(name, password);

        }
    }
}

@Composable
fun LoginButton(name: String, password: String) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val userDao = database.userDao()
    val coroutineScope = rememberCoroutineScope()

    Button(
        modifier = Modifier.offset((-10).dp),
        onClick = {
            if (name.isNotEmpty() && password.isNotEmpty()) {
                coroutineScope.launch(Dispatchers.IO) {
                    val user = userDao.getUser(name)
                    if (user != null) {
                        val isPasswordCorrect = comparePassword(password, user.password)
                        if (isPasswordCorrect) {
                            context.startActivity(Intent(context, MainActivity::class.java))
                        } else {

                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Неверное имя пользователя или пароль", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {

                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Неверное имя пользователя или пароль", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Поля не заполнены", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Войти")
    }
}
@Composable
fun RegisterButton(name: String, password: String) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val userDao = db.userDao()
    val passwordHash = hashPassword(password)
    Button(
        modifier = Modifier.offset((10).dp),
        onClick = {
            if (!name.isEmpty() && !password.isEmpty()) {
                val newUser = UserEntity(username = name, password = passwordHash)
                CoroutineScope(Dispatchers.IO).launch {
                    userDao.insertUser(newUser)
                    context.startActivity(Intent(context, MainActivity::class.java))
                }
            } else {
                Toast.makeText(context, "Поля не заполнены", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Регистрация")
    }
}

@Preview
@Composable
fun WelcomeScreenPreview(){
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Добро пожаловать!",
            fontSize = 24.sp
        )
        Spacer(
            modifier = Modifier
                .height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = {Text("Введите ваше имя: ")},
            modifier = Modifier
                .fillMaxWidth(),

            )
        Spacer(
            modifier = Modifier
                .height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {password = it},
            label = {Text("Введите пароль: ")},
            modifier = Modifier
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))
    Row () {
        LoginButton(name, password)
        RegisterButton(name, password);
    }


    }
}

