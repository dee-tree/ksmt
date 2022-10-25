package org.ksmt.runner.serializer

import com.jetbrains.rd.framework.FrameworkMarshallers
import com.jetbrains.rd.framework.RdId
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.getPlatformIndependentHash
import org.ksmt.KAst
import org.ksmt.KContext

class AstSerializationCtx {
    private var context: KContext? = null

    private val serializedAst = hashMapOf<KAst, Int>()
    private val deserializedAst = hashMapOf<Int, KAst>()

    private var nextGeneratedIndex = 1

    val ctx: KContext
        get() = context ?: error("Serialization context is not initialized")

    fun initCtx(ctx: KContext) {
        check(context == null) { "Serialization context is initialized " }
        context = ctx
    }

    fun resetCtx() {
        serializedAst.clear()
        deserializedAst.clear()
        nextGeneratedIndex = 1
        context = null
    }

    fun mkAstIdx(ast: KAst): Int {
        check(ast !in serializedAst) { "Serialization failed: duplicate ast" }
        val idx = nextGeneratedIndex++
        saveAst(idx, ast)
        return idx
    }

    private fun saveAst(idx: Int, ast: KAst) {
        deserializedAst[idx] = ast
        serializedAst[ast] = idx
    }

    fun writeAst(idx: Int, ast: KAst) {
        val current = deserializedAst[idx]
        if (current != null) {
            check(current == ast) { "Serialization failed: different ast with same idx" }
            return
        }
        val reversed = serializedAst[ast]
        if (reversed != null) {
            check(deserializedAst[reversed] == ast) { "Serialization failed: cache mismatch" }
        }
        saveAst(idx, ast)
        nextGeneratedIndex = maxOf(nextGeneratedIndex, idx + 1)
    }

    fun getAstIndex(ast: KAst): Int? = serializedAst[ast]

    fun getAstByIndexOrError(idx: Int): KAst = deserializedAst[idx]
        ?: error("Serialization failed: $idx is not properly deserialized")

    companion object {
        const val SERIALIZED_DATA_END_IDX = -1
        const val SERIALIZED_AST_ENTRY_END = -2

        private val marshallerIdHash: Int by lazy {
            // convert to Int here since [FrameworkMarshallers.create] accepts an Int for id
            KAst::class.simpleName.getPlatformIndependentHash().toInt()
        }

        val marshallerId: RdId by lazy {
            RdId(marshallerIdHash.toLong())
        }

        fun marshaller(ctx: AstSerializationCtx) = FrameworkMarshallers.create<KAst>(
            writer = { buffer, ast ->
                AstSerializer(ctx, buffer).serializeAst(ast)
            },
            reader = { buffer ->
                AstDeserializer(ctx, buffer).deserializeAst()
            },
            predefinedId = marshallerIdHash
        )

        fun register(serializers: Serializers): AstSerializationCtx {
            val ctx = AstSerializationCtx()
            val marshaller = marshaller(ctx)
            serializers.register(marshaller)
            return ctx
        }
    }

}
