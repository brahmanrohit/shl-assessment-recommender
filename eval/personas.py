"""Self-authored evaluation personas (we weren't given the 10 official traces).

Each persona mirrors the real replay harness: an opening intent, a fact set the
simulated user answers from, and a labelled "relevant" shortlist (matchers resolved
against the live catalog) for Recall@10. Matchers are space-separated token sets;
an assessment matches if its name contains ALL tokens (case-insensitive)."""
from dataclasses import dataclass, field


@dataclass
class Persona:
    id: str
    opening: str
    facts: dict
    relevant: list[str] = field(default_factory=list)   # name matchers
    tag: str = "recommend"


PERSONAS = [
    Persona(
        id="java_dev",
        opening="I'm hiring a Java developer who works closely with business stakeholders.",
        facts={"seniority": "mid-level, about 4 years experience",
               "skills": "core Java, backend services, collaborates with stakeholders",
               "preference": "no strong preference on test length"},
        relevant=["core java advanced", "core java entry", "java 8", "java frameworks",
                  "java platform enterprise", "occupational personality questionnaire opq32r"],
        tag="recommend",
    ),
    Persona(
        id="python_ds",
        opening="We need to screen candidates for a data scientist role that uses Python.",
        facts={"skills": "Python, machine learning, SQL, statistics",
               "seniority": "mid-level", "preference": "no preference"},
        relevant=["python", "data science", "automata data science", "sql"],
        tag="recommend",
    ),
    Persona(
        id="cognitive_grad",
        opening="I want a general cognitive ability test battery for graduate hires.",
        facts={"level": "graduate scheme", "need": "numerical, verbal and inductive reasoning",
               "preference": "no preference"},
        relevant=["verify numerical reasoning", "verify verbal reasoning",
                  "verify inductive reasoning", "verify g+"],
        tag="recommend",
    ),
    Persona(
        id="office_admin",
        opening="I need to test administrative assistants on office software skills.",
        facts={"skills": "Microsoft Word, Excel, data entry, typing",
               "level": "entry level", "preference": "no preference"},
        relevant=["microsoft word", "microsoft excel", "microsoft outlook"],
        tag="recommend",
    ),
    Persona(
        id="dotnet_dev",
        opening="Here is a job description: We are hiring a .NET backend engineer with "
                "C# experience, building web services and working in an agile team.",
        facts={"skills": ".NET, C#, web services, agile", "seniority": "mid to senior",
               "preference": "no preference"},
        relevant=[".net framework", "c# programming", "asp .net", "agile software development"],
        tag="recommend",
    ),
    Persona(
        id="sales_personality",
        opening="I'm hiring sales representatives and I mainly care about personality and drive.",
        facts={"role": "field sales rep", "focus": "personality, motivation, resilience",
               "preference": "no preference"},
        # a reasonable sales personality/behaviour battery (not fitted to output)
        relevant=["occupational personality questionnaire opq32r",
                  "motivation questionnaire mq", "customer contact series questionnaire"],
        tag="recommend",
    ),
]
