package example.step2

import chipsalliance.rocketchip.config._
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.HasRocketChipStageUtils

trait DontOmitGraphML { this: LazyModule =>
  override def omitGraphML: Boolean = false //this.omitGraphML
}

class ExBlock(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  val rs = LazyModule(new ReservationStation())
  val exus = Seq(
    LazyModule(new Alu()),
    LazyModule(new Mdu())
  )
}

class ReservationStation(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML

abstract class Exu(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML
class Alu(implicit p: Parameters) extends Exu
class Mdu(implicit p: Parameters) extends Exu

class WriteBackArbiter(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML

class Backend(implicit p: Parameters) extends SimpleLazyModule {
  val exBlocks = Seq.fill(2){ LazyModule(new ExBlock()) }
  val wb_arb = LazyModule(new WriteBackArbiter())
}

object Backend extends App with HasRocketChipStageUtils {
  override def main(args: Array[String]): Unit = {
    implicit val config = Parameters.empty
    val backend = LazyModule(new Backend())
    val verilog = chisel3.stage.ChiselStage.emitVerilog(backend.module)
    writeOutputFile(".", "Backend.v", verilog)
    writeOutputFile(".", "Backend.graphml", backend.graphML)
  }
}

