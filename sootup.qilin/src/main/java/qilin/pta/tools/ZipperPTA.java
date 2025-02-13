/* Qilin - a Java Pointer Analysis Framework
 * Copyright (C) 2021-2030 Qilin developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3.0 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <https://www.gnu.org/licenses/lgpl-3.0.en.html>.
 */

package qilin.pta.tools;

import java.util.HashSet;
import java.util.Set;
import qilin.core.PTAScene;
import qilin.core.pag.*;
import qilin.parm.ctxcons.CtxConstructor;
import qilin.parm.heapabst.AllocSiteAbstractor;
import qilin.parm.heapabst.HeuristicAbstractor;
import qilin.parm.select.CtxSelector;
import qilin.parm.select.HeuristicSelector;
import qilin.parm.select.PartialMethodLvSelector;
import qilin.parm.select.PipelineSelector;
import qilin.pta.PTAConfig;
import qilin.pta.toolkits.zipper.Main;
import qilin.util.PTAUtils;
import qilin.util.Stopwatch;
import qilin.util.queue.QueueReader;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.InvokableStmt;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.model.SootMethod;
import sootup.core.types.ReferenceType;

/*
 * refer to "Precision-Guided Context Sensitivity for Pointer Analysis" (OOPSLA'18)
 * and "A Principled Approach to Selective Context Sensitivity for Pointer Analysis" (TOPLAS'20)
 * */
public class ZipperPTA extends StagedPTA {
  private final Set<SootMethod> PCMs = new HashSet<>();

  /*
   * Zipper support object-sensitivity, callsite-sensitivity by using corresponding
   * context-constructor.
   * */
  public ZipperPTA(PTAScene scene, int k, int hk, CtxConstructor ctxCons) {
    super(scene);
    this.ctxCons = ctxCons;
    CtxSelector us = new PartialMethodLvSelector(k, hk, PCMs);
    if (PTAConfig.v().getPtaConfig().enforceEmptyCtxForIgnoreTypes) {
      this.ctxSel = new PipelineSelector(new HeuristicSelector(getView()), us);
    } else {
      this.ctxSel = us;
    }
    if (PTAConfig.v().getPtaConfig().mergeHeap) {
      this.heapAbst = new HeuristicAbstractor(pag);
    } else {
      this.heapAbst = new AllocSiteAbstractor();
    }
    this.prePTA = new Spark(scene);
  }

  @Override
  protected void preAnalysis() {
    Stopwatch sparkTimer = Stopwatch.newAndStart("Spark");
    prePTA.pureRun();
    sparkTimer.stop();
    System.out.println(sparkTimer);
    Stopwatch zipperTimer = Stopwatch.newAndStart("Zipper");
    Main.run(prePTA, PCMs);
    zipperTimer.stop();
    System.out.println(zipperTimer);
    extraStats();
  }

  protected void extraStats() {
    int[] RM = new int[1], PCN = new int[1], NPCN = new int[1];
    int[] totalN = new int[1];
    for (ContextMethod momc : prePTA.getReachableMethods()) {
      SootMethod method = momc.method();
      Set<Object> nodes = new HashSet<>();

      if (!PTAUtils.hasBody(method)) {
        return;
      }
      MethodPAG srcmpag = pag.getMethodPAG(method);
      QueueReader<Node> reader = srcmpag.getInternalReader().clone();
      while (reader.hasNext()) {
        Node from = reader.next(), to = reader.next();
        if (from instanceof LocalVarNode) {
          nodes.add(((VarNode) from).getVariable());
        } else if (from instanceof AllocNode) {
          nodes.add(((AllocNode) from).getNewExpr());
        } else if (from instanceof FieldRefNode) {
          FieldRefNode fr = (FieldRefNode) from;
          VarNode base = fr.getBase();
          if (base instanceof LocalVarNode) {
            nodes.add(base.getVariable());
          }
        }

        if (to instanceof LocalVarNode) {
          nodes.add(((VarNode) to).getVariable());
        } else if (to instanceof FieldRefNode) {
          FieldRefNode fr = (FieldRefNode) to;
          VarNode base = fr.getBase();
          if (base instanceof LocalVarNode) {
            nodes.add(base.getVariable());
          }
        }
      }
      for (final InvokableStmt s : srcmpag.getInvokeStmts()) {
        AbstractInvokeExpr ie = s.getInvokeExpr().get();
        int numArgs = ie.getArgCount();
        for (int i = 0; i < numArgs; i++) {
          Value arg = ie.getArg(i);
          if (!(arg.getType() instanceof ReferenceType) || arg instanceof NullConstant) {
            continue;
          }
          nodes.add(arg);
        }

        if (s instanceof JAssignStmt) {
          Value dest = ((JAssignStmt) s).getLeftOp();
          if (dest.getType() instanceof ReferenceType) {
            nodes.add(dest);
          }
        }
        if (ie instanceof AbstractInstanceInvokeExpr) {
          AbstractInstanceInvokeExpr iie = (AbstractInstanceInvokeExpr) ie;
          Local base = iie.getBase();
          nodes.add(base);
        }
      }

      if (PCMs.contains(method)) {
        PCN[0] += nodes.size();
      } else {
        NPCN[0] += nodes.size();
      }
      RM[0]++;

      totalN[0] += nodes.size();
    }
    RM[0]--; // For the FakeMain method.
    System.out.println("#ReachableMethod:" + RM[0]);
    System.out.println("#FCSM:" + PCMs.size());
    System.out.println("#PCSM:0");
    System.out.println("#CIM:" + (RM[0] - PCMs.size()));
    System.out.println("#CIN: " + NPCN[0]);
    System.out.println("#CSN: " + PCN[0]);
    System.out.println("totalN: " + totalN[0]);
  }
}
