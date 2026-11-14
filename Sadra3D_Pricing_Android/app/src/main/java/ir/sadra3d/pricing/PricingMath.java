package ir.sadra3d.pricing;

/** Exact transcription of formulas from the supplied price 3d.xlsx workbook. */
public final class PricingMath {
    private PricingMath() {}

    public static final double DEFAULT_MATERIAL_PER_KG = 6000000;
    public static final double DEFAULT_MACHINE_PER_HOUR = 30000;
    public static final double DEFAULT_LABOR_PER_HOUR = 300000;
    public static final double LABOR_TIME_FRACTION = 0.05;

    public static final class Result {
        public final double material, machine, labor, net, profit, total;
        Result(double material, double machine, double labor, double net, double profit, double total) {
            this.material = material;
            this.machine = machine;
            this.labor = labor;
            this.net = net;
            this.profit = profit;
            this.total = total;
        }
    }

    public static Result calculate(double grams, double hours, double profitPercent,
                                   double materialPerKg, double machinePerHour, double laborPerHour) {
        grams = Math.max(0, grams);
        hours = Math.max(0, hours);
        materialPerKg = Math.max(0, materialPerKg);
        machinePerHour = Math.max(0, machinePerHour);
        laborPerHour = Math.max(0, laborPerHour);
        profitPercent = Math.max(0, profitPercent);
        // C3 = (A3*(B9/1000))+(B3*B10)+((B3*0.05)*B11)
        double material = grams * (materialPerKg / 1000d);
        double machine = hours * machinePerHour;
        double labor = (hours * LABOR_TIME_FRACTION) * laborPerHour;
        double net = material + machine + labor;
        // E3 = C3 + D3*C3/100
        double profit = profitPercent * net / 100d;
        return new Result(material, machine, labor, net, profit, net + profit);
    }
}
