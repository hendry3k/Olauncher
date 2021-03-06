package app.olauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.helper.isPackageInstalled
import app.olauncher.listener.OnSwipeTouchListener
import app.olauncher.listener.ViewSwipeTouchListener
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        populateHomeApps(false)
        viewModel.isOlauncherDefault()
    }

    override fun onClick(view: View) {
        when (view.id) {

            R.id.homeApp1 -> if (prefs.appPackage1.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName1, prefs.appPackage1)

            R.id.homeApp2 -> if (prefs.appPackage2.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName2, prefs.appPackage2)

            R.id.homeApp3 -> if (prefs.appPackage3.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName3, prefs.appPackage3)

            R.id.homeApp4 -> if (prefs.appPackage4.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName4, prefs.appPackage4)

            R.id.homeApp5 -> if (prefs.appPackage5.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName5, prefs.appPackage5)

            R.id.homeApp6 -> if (prefs.appPackage6.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName6, prefs.appPackage6)

            R.id.homeApp7 -> if (prefs.appPackage7.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName7, prefs.appPackage7)

            R.id.homeApp8 -> if (prefs.appPackage8.isEmpty()) onLongClick(view)
            else launchApp(prefs.appName8, prefs.appPackage8)

            R.id.clock -> openAlarmApp()
            R.id.date -> openCalendar()
            R.id.setDefaultLauncher -> viewModel.resetDefaultLauncherApp(requireContext())
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1 -> showAppList(Constants.FLAG_SET_HOME_APP_1)
            R.id.homeApp2 -> showAppList(Constants.FLAG_SET_HOME_APP_2)
            R.id.homeApp3 -> showAppList(Constants.FLAG_SET_HOME_APP_3)
            R.id.homeApp4 -> showAppList(Constants.FLAG_SET_HOME_APP_4)
            R.id.homeApp5 -> showAppList(Constants.FLAG_SET_HOME_APP_5)
            R.id.homeApp6 -> showAppList(Constants.FLAG_SET_HOME_APP_6)
            R.id.homeApp7 -> showAppList(Constants.FLAG_SET_HOME_APP_7)
            R.id.homeApp8 -> showAppList(Constants.FLAG_SET_HOME_APP_8)
        }
        return true
    }

    private fun initObservers() {
        viewModel.refreshHome.observe(viewLifecycleOwner, Observer<Boolean> {
            populateHomeApps(it)
        })
        viewModel.firstOpen.observe(viewLifecycleOwner, Observer<Boolean> { isFirstOpen ->
            if (isFirstOpen) {
                firstRunTips.visibility = View.VISIBLE
                setDefaultLauncher.visibility = View.GONE
            } else firstRunTips.visibility = View.GONE
        })
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner, Observer<Boolean> {
            if (firstRunTips.visibility == View.VISIBLE) return@Observer
            if (it) setDefaultLauncher.visibility = View.GONE
            else setDefaultLauncher.visibility = View.VISIBLE
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner, Observer<Int> {
            setHomeAlignment(it)
        })
    }

    private fun initSwipeTouchListener() {
        mainLayout.setOnTouchListener(getSwipeGestureListener(requireContext()))
        homeApp1.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp1))
        homeApp2.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp2))
        homeApp3.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp3))
        homeApp4.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp4))
        homeApp5.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp5))
        homeApp6.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp6))
        homeApp7.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp7))
        homeApp8.setOnTouchListener(getViewSwipeTouchListener(requireContext(), homeApp8))
    }

    private fun initClickListeners() {
        clock.setOnClickListener(this)
        date.setOnClickListener(this)
        setDefaultLauncher.setOnClickListener(this)
    }

    private fun setHomeAlignment(gravity: Int) {
        dateTimeLayout.gravity = gravity
        homeAppsLayout.gravity = gravity
        setDefaultLauncher.gravity = gravity
        homeApp1.gravity = gravity
        homeApp2.gravity = gravity
        homeApp3.gravity = gravity
        homeApp4.gravity = gravity
        homeApp5.gravity = gravity
        homeApp6.gravity = gravity
        homeApp7.gravity = gravity
        homeApp8.gravity = gravity
    }

    private fun populateHomeApps(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        val pm = requireContext().packageManager

        homeApp1.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp1, prefs.appName1, prefs.appPackage1, pm)) {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (homeAppsNum == 1) return

        homeApp2.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp2, prefs.appName2, prefs.appPackage2, pm)) {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (homeAppsNum == 2) return

        homeApp3.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp3, prefs.appName3, prefs.appPackage3, pm)) {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (homeAppsNum == 3) return

        homeApp4.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp4, prefs.appName4, prefs.appPackage4, pm)) {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }
        if (homeAppsNum == 4) return

        homeApp5.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp5, prefs.appName5, prefs.appPackage5, pm)) {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }
        if (homeAppsNum == 5) return

        homeApp6.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp6, prefs.appName6, prefs.appPackage6, pm)) {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }
        if (homeAppsNum == 6) return

        homeApp7.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp7, prefs.appName7, prefs.appPackage7, pm)) {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }
        if (homeAppsNum == 7) return

        homeApp8.visibility = View.VISIBLE
        if (!setHomeAppText(homeApp8, prefs.appName8, prefs.appPackage8, pm)) {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
    }

    private fun setHomeAppText(textView: TextView, appName: String, packageName: String, pm: PackageManager): Boolean {
        if (isPackageInstalled(packageName, pm)) {
            textView.text = appName
            return true
        }
        textView.text = ""
        return false
    }

    private fun hideHomeApps() {
        homeApp1.visibility = View.GONE
        homeApp2.visibility = View.GONE
        homeApp3.visibility = View.GONE
        homeApp4.visibility = View.GONE
        homeApp5.visibility = View.GONE
        homeApp6.visibility = View.GONE
        homeApp7.visibility = View.GONE
        homeApp8.visibility = View.GONE
    }

    private fun launchApp(appName: String, packageName: String) {
        viewModel.selectedApp(
            AppModel(appName, packageName),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun showAppList(flag: Int) {
        viewModel.getAppList()
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_appListFragment,
                bundleOf("flag" to flag)
            )
        } catch (e: Exception) {
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandNotificationDrawer(context: Context) {
        // Source: https://stackoverflow.com/a/51132142
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeLeftRight) return
        if (prefs.appPackageSwipeRight.isNotEmpty())
            launchApp(prefs.appNameSwipeRight, prefs.appPackageSwipeRight)
        else openDialerApp()
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftRight) return
        if (prefs.appPackageSwipeLeft.isNotEmpty())
            launchApp(prefs.appNameSwipeLeft, prefs.appPackageSwipeLeft)
        else openCameraApp()
    }

    private fun openDialerApp() {
        try {
            val sendIntent = Intent(Intent.ACTION_DIAL)
            startActivity(sendIntent)
        } catch (e: java.lang.Exception) {

        }
    }

    private fun openCameraApp() {
        try {
            val sendIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            startActivity(sendIntent)
        } catch (e: java.lang.Exception) {

        }
    }

    private fun openAlarmApp() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            startActivity(intent)
        } catch (e: java.lang.Exception) {
            Log.d("TAG", e.toString())
        }
    }

    private fun openCalendar() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            startActivity(intent)
        } catch (e: java.lang.Exception) {

        }
    }

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                expandNotificationDrawer(context)
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: java.lang.Exception) {
                }
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                expandNotificationDrawer(context)
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }
}