package example.step1

import chipsalliance.rocketchip.config._
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.HasRocketChipStageUtils

class Backend(implicit p: Parameters) extends SimpleLazyModule {

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
