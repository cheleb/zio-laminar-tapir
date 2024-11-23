package dev.cheleb.ziochimney

import zio.Task
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.Transformer

/** Extension methods for `Task` that allow to transform the result of the task
  * using Chimney.
  */
extension [A](task: Task[A])
  def mapInto[B](using Transformer[A, B]): Task[B] =
    task.map(_.into[B].transform)

/** Extension methods for `Task[Option[A]]` that allow to transform the result
  * of the task using Chimney.
  */
extension [A](task: Task[Option[A]])
  def mapOption[B](using Transformer[A, B]): Task[Option[B]] =
    task.map(_.map(_.into[B].transform))
