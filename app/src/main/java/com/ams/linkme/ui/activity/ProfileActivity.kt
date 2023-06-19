package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R
import com.ams.linkme.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth


class ProfileActivity : AppCompatActivity() {
    private lateinit var radioButtonMale: RadioButton
    private lateinit var imageViewPerson: ImageView
    private lateinit var radioButtonFemale: RadioButton
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var editTextPhone: EditText
    private lateinit var submitButton: Button
    private lateinit var sport: CheckBox
    private lateinit var food: CheckBox
    private lateinit var dating: CheckBox
    private lateinit var games: CheckBox
    private lateinit var music: CheckBox
    private lateinit var movie: CheckBox
    private lateinit var anime: CheckBox
    private lateinit var other: CheckBox
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var gotolink: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize all views
        radioButtonMale = findViewById(R.id.radio_button_male)
        imageViewPerson = findViewById(R.id.imageView)
        radioButtonFemale = findViewById(R.id.radio_button_female)
        radioGroupGender = findViewById(R.id.radio_group_gender)
        editTextPhone = findViewById(R.id.editTextPhone)
        submitButton = findViewById(R.id.button)
        sport = findViewById(R.id.Sports)
        food = findViewById(R.id.Food)
        dating = findViewById(R.id.Dating)
        games = findViewById(R.id.Games)
        music = findViewById(R.id.Music)
        movie = findViewById(R.id.Movie)
        anime = findViewById(R.id.Anime)
        other = findViewById(R.id.Other)
        gotolink = findViewById(R.id.btnLink)
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        submitButton.setOnClickListener {
            // Get selected radio button from radioGroup
            val gender = when (radioGroupGender.checkedRadioButtonId) {
                R.id.radio_button_male -> "Male"
                R.id.radio_button_female -> "Female"
                else -> "Not Specified"
            }
            // Get inputs from edit texts
            val phone = editTextPhone.text.toString().trim()
            val interests = ArrayList<String>()
            if (sport.isChecked) {
                interests.add("Sport")
            }
            if (food.isChecked) {
                interests.add("Food")
            }
            if (dating.isChecked) {
                interests.add("Dating")
            }
            if (games.isChecked) {
                interests.add("Games")
            }
            if (music.isChecked) {
                interests.add("Music")
            }
            if (anime.isChecked) {
                interests.add("Anime")
            }
            if (movie.isChecked) {
                interests.add("Movie")
            }
            if (other.isChecked) {
                interests.add("Other")
            }
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val uid = user!!.uid // The user's ID, unique to the Firebase project.
            profileViewModel.submit(uid, gender, phone, interests)
            Toast.makeText(this,"提交成功",Toast.LENGTH_SHORT).show()
            navigateToMainActivity()
        }

        gotolink.setOnClickListener{
            navigateToMainActivity()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bottom_navigation_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_link -> {
                navigateToMainActivity()
                true
            }

            R.id.menu_chat -> {
                navigateToChatActivity()
                true
            }

            R.id.menu_profile -> {
                Toast.makeText(this, "你已经在这一页了", Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@ProfileActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToChatActivity() {
        val intent = Intent(this@ProfileActivity, ChatActivity::class.java)
        startActivity(intent)
        finish()
    }
}

