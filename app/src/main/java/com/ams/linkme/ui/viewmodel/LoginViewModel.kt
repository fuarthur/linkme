import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val loginResultLiveData: MutableLiveData<LoginResult> = MutableLiveData()

    fun login(email: String, password: String) {
        // 执行登录逻辑
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 登录成功
                    loginResultLiveData.value = LoginResult.Success
                } else {
                    // 登录失败
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    loginResultLiveData.value = LoginResult.Error(errorMessage)
                }
            }
    }

    sealed class LoginResult {
        object Success : LoginResult()
        data class Error(val errorMessage: String) : LoginResult()
    }
}
