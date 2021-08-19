package de.romqu.schimmelhofapi.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class Result<out F, out S> {

    data class Success<out S>(val data: S) : Result<Nothing, S>()

    data class Failure<out F>(val failure: F) : Result<F, Nothing>()


    inline fun <R : Any> doOn(success: (S) -> R, failure: (F) -> R): R =
        when (this) {
            is Failure -> failure(this.failure)
            is Success -> success(data)
        }

    fun unwrapOrNull(): S? =
        when (this) {
            is Failure -> null
            is Success -> data
        }

    companion object {

        inline fun <S1, S2, F, R> zip(
            result1: Result<F, S1>,
            result2: Result<F, S2>,
            transform: (S1, S2) -> R,
        ): Result<F, R> =
            result1.flatMap { s1 ->
                result2.map { s2 ->
                    transform(s1, s2)
                }
            }
    }
}


@ExperimentalContracts
fun <F, S> Result<F, S>.isFailure(): Boolean {
    contract {
        returns(true) implies (this@isFailure is Result.Failure<F>)
    }
    return this is Result.Failure<F>
}

// since F is defined as 'out' it has to be an extension function
inline fun <F, S, R> Result<F, S>.doOn(
    success: (S) -> Result<F, R>,
    withFailureCondition: (F) -> Boolean,
    failure: (F) -> Result<F, R>,
): Result<F, R> =
    when (this) {
        is Result.Failure -> if (withFailureCondition(this.failure)) failure(this.failure) else this
        is Result.Success -> success(data)
    }

inline fun <F, S, R, F2> Result<F, S>.doOnResult(
    success: (S) -> R,
    failure: (F) -> F2,
): Result<F2, R> =
    when (this) {
        is Result.Failure -> Result.Failure(failure(this.failure))
        is Result.Success -> Result.Success(success(data))
    }

inline fun <F, S, reified E : Any> Result<F, S>.doOnFailureElseContinue(
    failureType: E,
    `do`: (E) -> Result<F, S>,
): Result<F, S> =
    when (this) {
        is Result.Failure -> if (failure is E) `do`(this.failure) else this
        is Result.Success -> this
    }

inline fun <F, S, reified F2 : F> Result<F, S>.doIfFailureElseContinue(
    isOfType: (F) -> Boolean = { it is F2 },
    `do`: (F2) -> Result<F, S>,
): Result<F, S> =
    when (this) {
        is Result.Failure -> if (isOfType(this.failure)) `do`(this.failure as F2) else this
        is Result.Success -> this
    }

inline fun <F, S, R, reified FT : Any> Result<F, S>.doOn(
    success: (S) -> Result<F, R>,
    withFailureType: FT,
    failure: (F) -> Result<F, R>,
): Result<F, R> =
    when (this) {
        is Result.Failure -> if (this.failure is FT) failure(this.failure) else this
        is Result.Success -> success(data)
    }

fun <F, S> List<Result<F, List<S>>>.reduceResult(): Result<F, List<S>> =
    reduce { previousResult, currentResult ->
        previousResult.flatMap { previousList ->
            currentResult.map { currentList ->
                previousList.union(currentList).toList()
            }
        }
    }


@ExperimentalContracts
fun <F, S> List<Result<F, S>>.getFirstResultFailureOrNull(): Result<F, S>? =
    find { it.isFailure() }

@ExperimentalContracts
fun <F, S> List<Result<F, S>>.getFirstFailureOrNull(): Result.Failure<F>? =
    find { it.isFailure() } as? Result.Failure


@ExperimentalContracts
fun <F, S> List<Result<F, S>>.containsFailure() =
    getFirstResultFailureOrNull() != null

fun <F, S, R> List<Result<F, S>>.mapResult(transform: (S) -> R) {
    map { result ->
        result.map(transform)
    }
}

inline fun <F, S, R> Result<F, S>.flatMap(
    transform: (S) -> Result<F, R>,
): Result<F, R> =
    doOn(transform, { this as Result.Failure })

inline fun <F, S, R, F2> Result<F, S>.flatMapError(
    error: F2,
    transform: (S) -> Result<F2, R>,
): Result<F2, R> =
    doOn(transform, { Result.Failure(error) })

inline fun <F, S, R, F2> Result<F, S>.flatMapError(
    transform: (S) -> Result<F2, R>,
    transformError: (F) -> F2,
): Result<F2, R> =
    doOn(transform, { Result.Failure(transformError((this as Result.Failure).failure)) })

inline fun <F, S, R> Result<F, S>.map(transform: (S) -> R): Result<F, R> =
    flatMap { Result.Success(transform(it)) }

inline fun <F, S, R, F2> Result<F, S>.mapError(
    error: F2,
    transform: (S) -> R = { it as R },
): Result<F2, R> =
    flatMapError(error) { Result.Success(transform(it)) }

inline fun <F, S, R, F2> Result<F, S>.mapWithError(
    transform: (S) -> R = { it as R },
    transformError: (F) -> F2,
): Result<F2, R> =
    flatMapError({ Result.Success(transform(it)) }, transformError)

inline fun <S1, S2, F, R> Result<F, S1>.zipWith(
    result2: Result<F, S2>,
    transform: (S1, S2) -> R,
): Result<F, R> = Result.zip(this, result2, transform)

inline fun <F, S, R> Result<F, S>.asyncFlatMap(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    crossinline block: (S) -> R,
): Result<F, S> {

    scope.launch(dispatcher) {
        this@asyncFlatMap.map { block(it) }
    }

    return this@asyncFlatMap
}

inline fun <F, S, R> Result<F, S>.asyncMap(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    crossinline block: (S) -> R,
): Result<F, S> {

    scope.launch(dispatcher) {
        this@asyncMap.map { block(it) }
    }

    return this@asyncMap
}