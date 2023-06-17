package com.ams.linkme.ui.activity

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R
import com.ams.linkme.ui.viewmodel.ProfileViewModel


class ProfileActivity : AppCompatActivity() {
    private var profileViewModel: ProfileViewModel? = null
    private var interestsListView: ListView? = null
    private var nameEditText: EditText? = null
    private var ageEditText: EditText? = null
    private var genderEditText: EditText? = null
    private var submitButton: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        interestsListView = findViewById<ListView>(R.id.interestsListView)
        nameEditText = findViewById<EditText>(R.id.nameEditText)
        ageEditText = findViewById<EditText>(R.id.ageEditText)
        genderEditText = findViewById<EditText>(R.id.genderEditText)
        submitButton = findViewById<Button>(R.id.submitButton)

        // 设置按钮勾选的兴趣供用户选择
        val interests = arrayOf("Interest 1", "Interest 2", "Interest 3")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_multiple_choice, interests
        )
        interestsListView.setAdapter(adapter)
        interestsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE)

        // 监听按钮勾选的兴趣
        interestsListView.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            val selectedItems = interestsListView.getCheckedItemPositions()
            val selectedInterests: MutableList<String> = ArrayList()
            for (i in 0 until selectedItems.size()) {
                if (selectedItems.valueAt(i)) {
                    selectedInterests.add(interests[selectedItems.keyAt(i)])
                }
            }
            profileViewModel!!.interests.setValue(selectedInterests)
        })

        // 监听输入框的文本变化
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                profileViewModel!!.name.setValue(s.toString())
            }

            override fun afterTextChanged(s: Editable) {}
        })
        ageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val age = Integer.valueOf(s.toString())
                profileViewModel!!.age.setValue(age)
            }

            override fun afterTextChanged(s: Editable) {}
        })
        genderEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                profileViewModel!!.gender.setValue(s.toString())
            }

            override fun afterTextChanged(s: Editable) {}
        })

        // 监听提交按钮点击事件
        submitButton.setOnClickListener(object : OnClickListener() {
            fun onClick(v: View?) {
                profileViewModel!!.submit()
            }
        })
    }
}

