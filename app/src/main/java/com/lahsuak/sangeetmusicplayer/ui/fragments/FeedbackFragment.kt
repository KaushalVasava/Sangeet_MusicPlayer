package com.lahsuak.sangeetmusicplayer.ui.fragments


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lahsuak.sangeetmusicplayer.R
import com.lahsuak.sangeetmusicplayer.databinding.FragmentFeedbackBinding


class FeedbackFragment : Fragment(R.layout.fragment_feedback) {
    private lateinit var binding: FragmentFeedbackBinding
//    private val handler = Handler(Looper.getMainLooper())
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
         binding=FragmentFeedbackBinding.inflate(inflater, container, false)

         binding.submitButton.setOnClickListener {
             val feedbackMsg = binding.mailDescription.text.toString()
         //            binding.mailId.text.toString()
             val subject = binding.mailSubject.text.toString()
             try {
                 val mail: Array<String> = arrayOf(requireContext().getString(R.string.email))
                 val mailme = Intent(Intent.ACTION_SENDTO).apply {
                     data = Uri.parse("mailto:")
                     putExtra(Intent.EXTRA_EMAIL, mail)
                     putExtra(Intent.EXTRA_TEXT, feedbackMsg)
                     putExtra(Intent.EXTRA_SUBJECT, subject)
                 }
                 startActivity(mailme)
             } catch (e: Exception) {
                 e.printStackTrace()
             }
             findNavController().popBackStack()
         }
//         binding.submitButton.setOnClickListener {
//             val feedbackMsg = binding.mailDescription.text.toString()+"\n"+
//                     binding.mailId.text.toString()
//             val subject = binding.mailSubject.text.toString()
//             val userName = "vasavakaushal333@gmail.com"// "Sangeet - A Music Player"
//             val pass = "Ohmygod@333#129$001"
//             val cm = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//             if(feedbackMsg.isNotEmpty() && subject.isNotEmpty()
//                 && (cm.activeNetworkInfo?.isConnectedOrConnecting == true)) {
//               //  Handler(Looper.getMainLooper()).postDelayed({
//                    Thread{
//                     try {
//                         val properties = Properties()
//                         properties["mail.smtp.auth"] = "true"
//                         properties["mail.smtp.starttls.enable"] = "true"
//                         properties["mail.smtp.host"] = "smtp.hotmail.com"
//                         properties["mail.smtp.port"] = "587"
//
//                         val session = javax.mail.Session.getInstance(properties,
//                             object : javax.mail.Authenticator() {
//                                 override fun getPasswordAuthentication(): PasswordAuthentication {
//                                     return PasswordAuthentication(userName, pass)
//                                 }
//                             })
//                         val mail = MimeMessage(session)
//                         mail.subject = subject
//                         mail.setText(feedbackMsg)
//                         mail.setFrom(InternetAddress(userName))
//                         mail.setRecipients(
//                             Message.RecipientType.TO,
//                             InternetAddress.parse(userName)
//                         )
//                         Transport.send(mail)
//                     } catch (e: Exception) {
//                       //  Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show()
//                     }
//                 // },100)
////                     Thread {
////
//                 }.start()
//                 Toast.makeText(requireContext(), "Thanks for Feedback.", Toast.LENGTH_SHORT).show()
//             //    findNavController().popBackStack()
//             }
//             else{
//                 Toast.makeText(requireContext(), "Something went wrong!!", Toast.LENGTH_SHORT).show()
//             }
//         }
         return binding.root
    }
}