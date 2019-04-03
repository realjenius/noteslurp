package realjenius.evernote.noteslurp.reactor

import mu.KLogger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

fun <T,E> Flux<E>.schedulerMap(scheduler: Scheduler, op: (E) -> T) : Flux<T>
        = this.flatMap(_schedulerMono(scheduler, op))

fun <T,E> Mono<E>.schedulerMap(scheduler: Scheduler, op: (E) -> T) : Mono<T>
        = this.flatMap(_schedulerMono(scheduler,op))

fun <T> elasticMono(op: () -> T) = Mono.fromCallable(op).subscribeOn(Schedulers.elastic())

fun <E> Flux<E>.info(logger: KLogger, log: (E) -> String) = this.doOnNext(_info(logger, log))
fun <E> Mono<E>.info(logger: KLogger, log: (E) -> String) = this.doOnNext(_info(logger, log))
fun <E> Flux<E>.debug(logger: KLogger, log: (E) -> String) = this.doOnNext(_debug(logger, log))
fun <E> Mono<E>.debug(logger: KLogger, log: (E) -> String) = this.doOnNext(_debug(logger, log))


private fun <T,E> _schedulerMono(scheduler: Scheduler, op: (E) -> T) : (E) -> Mono<T> =
    { Mono.fromCallable { op(it) }.subscribeOn(scheduler) }

private fun <E> _info(logger: KLogger, log: (E) -> String) : (E) -> Unit = { logger.info { log(it) } }
private fun <E> _debug(logger: KLogger, log: (E) -> String) : (E) -> Unit = { logger.debug { log(it) } }

