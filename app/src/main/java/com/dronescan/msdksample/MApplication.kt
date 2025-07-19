package com.dronescan.msdksample // ¡IMPORTANTE! Este paquete DEBE coincidir con tu applicationId y la estructura de carpetas

import android.app.Application
import android.content.Context
import com.dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.SDKManager
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import android.util.Log

class MApplication : Application() {

    private val TAG = "MApplication"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MApplication onCreate called. Initializing DJI SDK...")

        // Inicializa el SDK de DJI
        SDKManager.getInstance().registerApp(this, object : SDKManager.SDKManagerCallback {
            override fun onRegister(error: DJIError?) {
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.d(TAG, "DJI SDK Registration Success!")
                    SDKManager.getInstance().startConnectionToProduct() // Inicia la conexión al producto DJI
                } else {
                    Log.e(TAG, "DJI SDK Registration Failed: ${error?.description}")
                    // Aquí podrías mostrar un mensaje al usuario o intentar de nuevo
                }
            }

            override fun onProductDisconnect() {
                Log.d(TAG, "Product Disconnected")
                // Aquí puedes manejar la desconexión del producto
            }

            override fun onProductConnect(baseProduct: BaseProduct?) {
                Log.d(TAG, "Product Connected: ${baseProduct?.model?.name}")
                // Aquí puedes manejar la conexión del producto
            }

            override fun onProductChanged(baseProduct: BaseProduct?) {
                Log.d(TAG, "Product Changed: ${baseProduct?.model?.name}")
                // Aquí puedes manejar el cambio de producto
            }

            override fun onComponentChange(
                component: BaseProduct.Component?,
                oldState: BaseProduct.Component.ComponentState?,
                newState: BaseProduct.Component.ComponentState?
            ) {
                Log.d(TAG, "Component Changed: ${component?.componentKey} from ${oldState?.productModel} to ${newState?.productModel}")
                // Aquí puedes manejar cambios en los componentes del dron (cámara, batería, etc.)
            }

            override fun onInitProcess(p0: SDKManager.SDKInitEvent?, p1: Int) {
                Log.d(TAG, "SDK Init Process: ${p0?.name}, progress: $p1%")
            }

            override fun onDatabaseDownloadAndVerify(p0: DJIError?) {
                if (p0 == null) {
                    Log.d(TAG, "Database Download and Verify Success")
                } else {
                    Log.e(TAG, "Database Download and Verify Failed: ${p0.description}")
                }
            }
        })
    }

    companion object {
        private var instance: MApplication? = null

        fun getInstance(): MApplication {
            if (instance == null) {
                throw IllegalStateException("Application not initialized yet.")
            }
            return instance!!
        }

        fun getApplication(): Context {
            return getInstance().applicationContext
        }
    }
}
