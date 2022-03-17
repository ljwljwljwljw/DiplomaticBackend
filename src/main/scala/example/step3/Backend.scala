package example.step3

import chipsalliance.rocketchip.config._
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.HasRocketChipStageUtils

trait DontOmitGraphML { this: LazyModule =>
  override def omitGraphML: Boolean = false //this.omitGraphML
}

class ExuInput extends Bundle
class ExuOutput extends Bundle

class ExBlock(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  val rs = LazyModule(new ReservationStation())
  val exus = Seq(
    LazyModule(new Alu()).suggestName("alu"),
    LazyModule(new Mdu()).suggestName("mdu")
  )
}

class Backend(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {

  val exBlocks = Seq.fill(2){ LazyModule(new ExBlock()) }
  val wb_arb = LazyModule(new WriteBackArbiter())

}

class ReservationStation(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  // source
  val issue_node = BundleBridgeNexusNode(Some(() => Decoupled(new ExuInput)))
  // sink
  val wakeup_node = BundleBridgeNexusNode[DecoupledIO[ExuOutput]]()
}

abstract class Exu(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  val input_node = BundleBridgeSink[DecoupledIO[ExuInput]]()
  val output_node = BundleBridgeSource(() => DecoupledIO(new ExuOutput))
}

class Alu(implicit p: Parameters) extends Exu
class Mdu(implicit p: Parameters) extends Exu

class WriteBackArbiter(implicit p: Parameters) extends SimpleLazyModule with DontOmitGraphML {
  val input_node = BundleBridgeNexusNode[DecoupledIO[ExuOutput]]()
}

object Backend extends App with HasRocketChipStageUtils {
  // not compile, since nodes haven't been connected
  override def main(args: Array[String]): Unit = {
    implicit val config = Parameters.empty
    val backend = LazyModule(new Backend())
    val verilog = chisel3.stage.ChiselStage.emitVerilog(backend.module)
    writeOutputFile(".", "Backend.v", verilog)
    writeOutputFile(".", "Backend.graphml", backend.graphML)
  }
}


