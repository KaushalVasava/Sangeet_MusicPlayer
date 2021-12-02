package com.lahsuak.sangeetmusicplayer.fragments


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.*
import com.lahsuak.sangeetmusicplayer.BuildConfig
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.ViewGroup
import android.widget.*
import android.widget.GridView
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.databinding.FragmentSettingsBinding


class Settings : Fragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding

    companion object {
        var whichTheme = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        "Version No: ${BuildConfig.VERSION_NAME}".also { binding.about.text = it }

        binding.themeSwitch.setOnClickListener {
            setTheme(container)
        }
        binding.theme.setOnClickListener {
            setTheme(container)
        }
        binding.shareApp.setOnClickListener {
            try{
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_SUBJECT,"Share Sangeet App")
                val shareMsg = "https://play.google.com/store/apps/details/id=?"+BuildConfig.APPLICATION_ID+"\n\n"
                intent.putExtra(Intent.EXTRA_TEXT,shareMsg)
                requireActivity().startActivity(Intent.createChooser(intent,"Share by"))
            }
            catch (e :Exception)
            {
                Toast.makeText(requireContext(), "Some thing went wrong!!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.feedback.setOnClickListener {
            val action =
                SettingsDirections.actionAllSettingsToFeedbackFragment()
            findNavController().navigate(action)
        }
        return binding.root
    }

    private fun setTheme(container: ViewGroup?){
        val themeDialog = LayoutInflater.from(requireContext())
            .inflate(R.layout.theme_dialog, container, false)
        val builder = MaterialAlertDialogBuilder(requireContext())
        val gridView:GridView = themeDialog.findViewById(R.id.gridview)
        gridView.adapter = ImageAdapter(requireContext())
        var storeTheme = 0

        gridView.setOnItemClickListener { _, _, position, _ ->
            whichTheme = position
            when(position){
                0-> storeTheme = R.style.Theme_MusicPlayer
                1-> storeTheme = R.style.Theme_MusicPlayerBlue
                2-> storeTheme = R.style.Theme_MusicPlayerRed
                3-> storeTheme = R.style.Theme_MusicPlayerPink
                4-> storeTheme = R.style.Theme_MusicPlayerGreen
            }
        }
        builder.setView(themeDialog)
            .setTitle("Select Theme")
            .setPositiveButton("Apply") { dialog, _ ->
                requireActivity().setTheme(storeTheme)

                when (whichTheme) {
                    0->{
                        binding.themeSwitch.setImageResource(R.color.purple_500)
                        Toast.makeText(requireContext(), "Purple theme applied", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        binding.themeSwitch.setImageResource(R.color.blue_500)
                        Toast.makeText(requireContext(), "Blue theme applied", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        binding.themeSwitch.setImageResource(R.color.red_500)
                        Toast.makeText(requireContext(), "Red theme applied", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        binding.themeSwitch.setImageResource(R.color.pink_500)
                        Toast.makeText(requireContext(), "Pink theme applied", Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        binding.themeSwitch.setImageResource(R.color.green_500)
                        Toast.makeText(requireContext(), "Green theme applied", Toast.LENGTH_SHORT).show()
                    }
                }
                val editor =
                    requireActivity().getSharedPreferences("THEME", MODE_PRIVATE).edit()
                editor.putInt("theme", storeTheme)
                editor.putInt("colorNo", whichTheme)
                editor.apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    class ImageAdapter(context: Context) : BaseAdapter() {
        private val mContext: Context = context
        override fun getCount(): Int {
            return mThumbIds.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        // create a new ImageView for each item referenced by the Adapter
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val imageView: ImageView
            if (convertView == null) {
                imageView = ImageView(mContext)
                imageView.layoutParams = AbsListView.LayoutParams(100, 100)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setPadding(8, 8, 8, 8)
            } else {
                imageView = convertView as ImageView
            }
            imageView.setImageResource(mThumbIds[position])
            return imageView
        }

        // Keep all Images in array
        var mThumbIds = arrayOf(
            R.drawable.purple_theme, R.drawable.blue_theme,
            R.drawable.red_theme, R.drawable.pink_theme,
            R.drawable.green_theme
        )

    }

}