#!/usr/bin/perl -w

use Getopt::Long;
use File::Basename;

my $prog = basename($0);

my $usage = "
  Program to parse the CSV output from the JMH benchmark and produce a table
  for insertion into JIRA.
  
Usage:

  $prog input [...]

Options:

  input     The JMH CSV report. This must be in unix file format on Linux.

  -help     Print this help and exit

";

my $help;
GetOptions(
	"help" => \$help,
);

die $usage if $help;

# Delete as necessary
my $input = shift or die $usage;

sub find($@) {
    local $target = "Param: " . shift;
    $i = 0;
    for (@_) {
        return $i if m/$target/;
        $i++;
    }
    warn "Failed to find columns for $target\n";
    return -1;
}

open (IN, $input) or die "Failed to open '$input': $!\n";
$header = readline(IN);
close IN;

# Find columns of interest
$iname = 0;
$iscore = 4;
@cols = split /,/, $header;
$imean = find("mean", @cols);
$isource = find("randomSourceName", @cols);
$irange = find("range", @cols);

die if $isource == -1;

# Single vs repeat use
if ($imean != -1) {

    open (IN, $input) or die "Failed to open '$input': $!\n";
    $header = readline(IN);
    while (<IN>)
    {
        chomp;
        s/"//g;
        s/^.*(Repeat|Single)Use_//;
        $bench = $1;
        @cols = split /,/, $_;
        
        $sources{$cols[$isource]} = 1;
        $means{$cols[$imean]} = 1;
        $data{$cols[$isource]}{$bench}{$cols[$imean]}{$cols[$iname]} = $cols[$iscore];
    }

    print "||Source||Use||Mean||Name||Relative Score||\n";
    for $source (sort keys %sources)
    {
        for $bench (sort keys %{$data{$source}}) 
        {
            for $mean (sort {$a<=>$b} keys %means)
            {
                $first = 0;
                for $name (sort {
                        $data{$source}{$bench}{$mean}{$b} <=> $data{$source}{$bench}{$mean}{$a}
                    } keys %{$data{$source}{$bench}{$mean}}) 
                {
                    $score = $data{$source}{$bench}{$mean}{$name};
                    if ($first) {
                        $rel = $score / $first;
                    } else {
                        $rel = 1;
                        $first = $score;
                    }
                    print "|$source|$bench|$mean|$name|$rel|\n";
                }
            }
        }
    }

    # Do it again to compare single & repeats use
    open (IN, $input) or die "Failed to open '$input': $!\n";
    $header = readline(IN);

    my %data;

    while (<IN>)
    {
        chomp;
        s/"//g;
        s/^.*(Repeat|Single)Use_/$1 /;
        @cols = split /,/, $_;
        
        $score = ($1 eq 'Repeat') ? $cols[$iscore] : $cols[$iscore] * 10;
        $data{$cols[$isource]}{$cols[$imean]}{$cols[$iname]} = $score;
    }
    close IN;

    print "||Source||Mean||Name||Relative Score||\n";
    for $source (sort keys %sources)
    {
        for $mean (sort {$a<=>$b} keys %means)
        {
            # Do relative to the original
            $first = $data{$source}{$mean}{'Repeat PoissonSampler'};
            for $name (sort {
                    $data{$source}{$mean}{$b} <=> $data{$source}{$mean}{$a}
                } keys %{$data{$source}{$mean}}) 
            {
                $score = $data{$source}{$mean}{$name};
                if ($first) {
                    $rel = $score / $first;
                } else {
                    $rel = 1;
                    $first = $score;
                }
                print "|$source|$mean|$name|$rel|\n";
            }
        }
    }

} 

# Single vs repeat use
if ($irange != -1) {

    open (IN, $input) or die "Failed to open '$input': $!\n";
    $header = readline(IN);
    my %data;
    while (<IN>)
    {
        chomp;
        s/"//g;
        s/^.*runPoissonSamplerCache_//;
        @cols = split /,/, $_;
        
        $sources{$cols[$isource]} = 1;
        $ranges{$cols[$irange]} = 1;
        $data{$cols[$isource]}{$cols[$irange]}{$cols[$iname]} = $cols[$iscore];
    }

    print "||Source||Range||Name||Relative Score||\n";
    for $source (sort keys %sources)
    {
        for $range (sort {$a<=>$b} keys %ranges)
        {
            $first = 0;
            for $name (sort {
                    $data{$source}{$range}{$b} <=> $data{$source}{$range}{$a}
                } keys %{$data{$source}{$range}}) 
            {
                $score = $data{$source}{$range}{$name};
                if ($first) {
                    $rel = $score / $first;
                } else {
                    $rel = 1;
                    $first = $score;
                }
                print "|$source|$range|$name|$rel|\n";
            }
        }
    }
}

