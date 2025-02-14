package com.xiaoyv.java.compiler.tools.exec

import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.xiaoyv.java.compiler.JavaEngine
import com.xiaoyv.java.compiler.JavaEngineSetting
import com.xiaoyv.java.compiler.exception.CompileException
import dalvik.system.DexClassLoader
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * JavaProgram
 *
 * @author why
 * @since 2022/3/8
 */
class JavaProgram {

    private val defaultChooseMainClassToRun: (List<String>, CancellableContinuation<String>) -> Unit =
        { mainFunClasses, continuation ->
            if (mainFunClasses.isEmpty()) {
                continuation.resumeWithException(CompileException("未找到包含 main(String[] args) 方法的可执行类"))
            } else {
                continuation.resume(mainFunClasses.first())
            }
        }

    /**
     * 运行 Dex 文件
     *
     * ### [chooseMainClassToRun]
     * - 第一个回调参数为查询到的包含 main(String[] args) 方法的所有类全路径
     * - 第二个回调参数为 [CancellableContinuation] ，将选取的类名通过 `continuation.resume()` 方法回调。
     *
     * > 若长时间未回调选择结果，协程则会一直挂起，占用资源。请及时回调或者取消 `continuation.cancel()`
     *
     * @param dexFile 文件路径
     * @param args 文件参数
     * @param chooseMainClassToRun 选取一个主类进行运行
     * @return [JavaProgramConsole] 可以关闭该程序相关句柄
     */
    suspend fun run(
        dexFile: String,
        args: Array<String> = emptyArray(),
        chooseMainClassToRun: (List<String>, CancellableContinuation<String>) -> Unit = defaultChooseMainClassToRun,
        printOut: (CharSequence) -> Unit = { },
        printErr: (CharSequence) -> Unit = { },
    ) = run(File(dexFile), args, chooseMainClassToRun, printOut, printErr)

    suspend fun run(
        dexFile: File,
        args: Array<String> = emptyArray(),
        chooseMainClassToRun: (List<String>, CancellableContinuation<String>) -> Unit = defaultChooseMainClassToRun,
        printOut: (CharSequence) -> Unit = { },
        printErr: (CharSequence) -> Unit = { },
    ) = withContext(Dispatchers.IO) {
        JavaEngine.resetProgram()

        // 包含的全部 Main 方法
        val mainFunctionList = JavaProgramHelper.queryMainFunctionList(dexFile)

        // 选者的 Fun
        val mainClass = suspendCancellableCoroutine<String> {
            launch(Dispatchers.Main) {
                chooseMainClassToRun.invoke(mainFunctionList, it)
            }
        }

        val optimizedDirectory = JavaEngineSetting.defaultCacheDir
        val dexClassLoader = DexClassLoader(
            dexFile.absolutePath, optimizedDirectory, null, ClassLoader.getSystemClassLoader()
        )

        // 加载 Class
        val clazz = dexClassLoader.loadClass(mainClass)
        // 获取 main 方法
        val method = clazz.getDeclaredMethod("main", Array<String>::class.java)

        JavaProgramConsole().apply {
            logNormalListener = {
                runCatching {
                    // 输出样式
                    val colorSpan = ForegroundColorSpan(JavaEngine.compilerSetting.normalLogColor)
                    printOut.invoke(SpannableStringBuilder().apply {
                        append(it)
                        setSpan(colorSpan, 0, it.length, 0)
                    })
                }
            }
            logErrorListener = {
                runCatching {
                    // 错误样式
                    val colorSpan = ForegroundColorSpan(JavaEngine.compilerSetting.errorLogColor)
                    printErr.invoke(SpannableStringBuilder().apply {
                        append(it)
                        setSpan(colorSpan, 0, it.length, 0)
                    })
                }
            }

            // 开启日志代理
            interceptSystemPrint()

            JavaEngine.lastProgram = this

            launch(Dispatchers.IO) {
                // 调用静态方法可以直接传 null
                delay(100)
                method.invoke(null, args)
                delay(100)
            }
        }
    }
}