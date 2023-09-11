package io.ksmt.solver.bitwuzla

import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverUniversalConfigurationBuilder
import io.ksmt.solver.KSolverUnsupportedParameterException
import org.ksmt.solver.bitwuzla.bindings.Bitwuzla
import org.ksmt.solver.bitwuzla.bindings.BitwuzlaOption
import org.ksmt.solver.bitwuzla.bindings.BitwuzlaOptionsNative
import org.ksmt.solver.bitwuzla.bindings.Native
import java.util.EnumMap

interface KBitwuzlaSolverConfiguration : KSolverConfiguration {
    fun setBitwuzlaOption(option: BitwuzlaOption, value: Long)
    fun setBitwuzlaOption(option: BitwuzlaOption, value: String)

    override fun setIntParameter(param: String, value: Int) {
        val option = BitwuzlaOption.forName(param)
            ?: throw KSolverUnsupportedParameterException("Int parameter $param is not supported in Bitwuzla")

        setBitwuzlaOption(option, value.toLong())
    }

    override fun setStringParameter(param: String, value: String) {
        val option = BitwuzlaOption.forName(param)
            ?: throw KSolverUnsupportedParameterException("String parameter $param is not supported in Bitwuzla")

        setBitwuzlaOption(option, value)
    }

    override fun setBoolParameter(param: String, value: Boolean) {
        throw KSolverUnsupportedParameterException("Boolean parameter $param is not supported in Bitwuzla")
    }

    override fun setDoubleParameter(param: String, value: Double) {
        throw KSolverUnsupportedParameterException("Double parameter $param is not supported in Bitwuzla")
    }
}

class KBitwuzlaSolverConfigurationImpl(private val bitwuzlaOptions: BitwuzlaOptionsNative) : KBitwuzlaSolverConfiguration {
    override fun setBitwuzlaOption(option: BitwuzlaOption, value: Long) {
        Native.bitwuzlaSetOption(bitwuzlaOptions, option, value)
    }

    override fun setBitwuzlaOption(option: BitwuzlaOption, value: String) {
        Native.bitwuzlaSetOptionMode(bitwuzlaOptions, option, value)
    }
}

class KBitwuzlaForkingSolverConfigurationImpl(private val bitwuzla: Bitwuzla) : KBitwuzlaSolverConfiguration {
    private val longOptions = EnumMap<_, Long>(BitwuzlaOption::class.java)
    private val stringOptions = EnumMap<_, String>(BitwuzlaOption::class.java)
    override fun setBitwuzlaOption(option: BitwuzlaOption, value: Long) {
        Native.bitwuzlaSetOption(bitwuzla, option, value)
        longOptions[option] = value
    }

    override fun setBitwuzlaOption(option: BitwuzlaOption, value: String) {
        Native.bitwuzlaSetOptionMode(bitwuzla, option, value)
        stringOptions[option] = value
    }

    fun fork(childBitwuzla: Bitwuzla) = KBitwuzlaForkingSolverConfigurationImpl(childBitwuzla).also {
        longOptions.forEach { (option, value) -> it.setBitwuzlaOption(option, value) }
        stringOptions.forEach { (option, value) -> it.setBitwuzlaOption(option, value) }
    }
}

class KBitwuzlaSolverUniversalConfiguration(
    private val builder: KSolverUniversalConfigurationBuilder
) : KBitwuzlaSolverConfiguration {
    override fun setBitwuzlaOption(option: BitwuzlaOption, value: Long) {
        builder.buildIntParameter(option.name, value.toInt())
    }

    override fun setBitwuzlaOption(option: BitwuzlaOption, value: String) {
        builder.buildStringParameter(option.name, value)
    }
}
